package com.joshuawallis.jwplayer.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joshuawallis.jwplayer.data.SettingsRepository
import com.joshuawallis.jwplayer.playback.PlaybackViewModel
import com.joshuawallis.jwplayer.ui.screens.main.MainScreen
import com.joshuawallis.jwplayer.ui.screens.settings.SettingsScreen
import java.io.File

object Route {
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(
    playbackViewModel: PlaybackViewModel,
    settingsRepository: SettingsRepository,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current

    var rootFolderUri by remember { mutableStateOf(settingsRepository.getRootFolderUri()) }
    var whiteNoiseUri by remember { mutableStateOf(settingsRepository.getWhiteNoiseUri()) }
    val rootFolderDoc =
        remember(rootFolderUri) {
            rootFolderUri?.let { uri ->
                // A "file" scheme means the debug-only test backdoor set this
                // (see debug/TestSetRootFolderReceiver.kt) - a plain path, not
                // a real SAF tree grant, so it needs DocumentFile.fromFile
                // instead of fromTreeUri. Never happens in a release build,
                // since that receiver only exists in the debug source set.
                if (uri.scheme == "file") {
                    uri.path?.let { path -> DocumentFile.fromFile(File(path)) }
                } else {
                    DocumentFile.fromTreeUri(context, uri)
                }
            }
        }
    // Saved as the chain of folder URIs from the root, since a DocumentFile rebuilt
    // from a bare URI has no parentFile; restoring re-walks the chain from the root.
    val currentFolderSaver =
        Saver<DocumentFile?, ArrayList<String>>(
            save = { folder ->
                folder?.let { ArrayList(FolderChain.idsFromRoot(it, { doc -> doc.uri.toString() }, { doc -> doc.parentFile })) }
            },
            restore = { ids ->
                rootFolderDoc?.let { root ->
                    FolderChain.resolve(root, ids, { doc -> doc.uri.toString() }, { doc -> doc.listFiles().asList() })
                }
            },
        )
    var currentFolderDoc by rememberSaveable(rootFolderUri, stateSaver = currentFolderSaver) {
        mutableStateOf(rootFolderDoc)
    }

    NavHost(navController = navController, startDestination = Route.MAIN) {
        composable(Route.MAIN) {
            BackHandler(enabled = currentFolderDoc?.uri != rootFolderDoc?.uri) {
                currentFolderDoc?.parentFile?.let { currentFolderDoc = it }
            }
            MainScreen(
                rootFolderDoc = rootFolderDoc,
                currentFolderDoc = currentFolderDoc,
                onFolderChange = { currentFolderDoc = it },
                playbackViewModel = playbackViewModel,
                onSettingsClick = { navController.navigate(Route.SETTINGS) },
            )
        }
        composable(Route.SETTINGS) {
            SettingsScreen(
                rootFolderUri = rootFolderUri,
                onRootFolderChosen = { uri ->
                    rootFolderUri = uri
                    settingsRepository.setRootFolderUri(uri)
                },
                whiteNoiseUri = whiteNoiseUri,
                onWhiteNoiseChosen = { uri ->
                    whiteNoiseUri = uri
                    settingsRepository.setWhiteNoiseUri(uri)
                },
                playbackViewModel = playbackViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
