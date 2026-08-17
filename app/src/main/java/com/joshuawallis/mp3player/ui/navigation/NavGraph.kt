package com.joshuawallis.mp3player.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joshuawallis.mp3player.data.SettingsRepository
import com.joshuawallis.mp3player.playback.PlaybackViewModel
import com.joshuawallis.mp3player.ui.screens.main.MainScreen
import com.joshuawallis.mp3player.ui.screens.settings.SettingsScreen

object Route {
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(
    playbackViewModel: PlaybackViewModel,
    settingsRepository: SettingsRepository,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    var rootFolderUri by remember { mutableStateOf(settingsRepository.getRootFolderUri()) }
    var whiteNoiseUri by remember { mutableStateOf(settingsRepository.getWhiteNoiseUri()) }
    val rootFolderDoc = remember(rootFolderUri) {
        rootFolderUri?.let { DocumentFile.fromTreeUri(context, it) }
    }
    var currentFolderDoc by remember(rootFolderDoc) { mutableStateOf(rootFolderDoc) }

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
                onSettingsClick = { navController.navigate(Route.SETTINGS) }
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}
