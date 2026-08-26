package com.joshuawallis.jwplayer.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.joshuawallis.jwplayer.playback.PlaybackMode
import com.joshuawallis.jwplayer.playback.PlaybackViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    rootFolderUri: Uri?,
    onRootFolderChosen: (Uri) -> Unit,
    whiteNoiseUri: Uri?,
    onWhiteNoiseChosen: (Uri) -> Unit,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uiState by playbackViewModel.uiState.collectAsState()
    val whiteNoisePlaying = uiState.mode == PlaybackMode.WHITE_NOISE && uiState.isPlaying

    val folderPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onRootFolderChosen(uri)
            }
        }

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onWhiteNoiseChosen(uri)
            }
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(innerPadding)
                    .padding(16.dp),
        ) {
            Text("White Noise", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val whiteNoiseFileName = whiteNoiseUri?.let { singleDocumentName(context, it) }
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                    modifier =
                        Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = whiteNoiseFileName?.let { "file $it" } ?: "Select white noise file"
                            },
                ) {
                    Text(
                        text = whiteNoiseFileName ?: "Select File",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("white_noise_file_button"),
                    )
                }

                Button(
                    onClick = { playbackViewModel.toggleWhiteNoise(whiteNoiseUri) },
                    modifier = Modifier.testTag(if (whiteNoisePlaying) "white_noise_pause_button" else "white_noise_play_button"),
                    colors =
                        if (whiteNoisePlaying) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary,
                                contentColor = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                ) {
                    Icon(
                        imageVector = if (whiteNoisePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (whiteNoisePlaying) "Pause white noise" else "Play white noise",
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Root Folder", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            val rootFolderName = rootFolderUri?.let { treeDocumentName(context, it) }
            Button(
                onClick = { folderPickerLauncher.launch(null) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = rootFolderName?.let { "folder $it" } ?: "Select root folder"
                        },
            ) {
                Text(
                    text = rootFolderName ?: "Select Folder",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("root_folder_button"),
                )
            }
        }
    }
}

private fun treeDocumentName(
    context: Context,
    treeUri: Uri,
): String {
    // A "file" scheme means the debug-only test backdoor set this URI
    // directly (see NavGraph.kt), not a real SAF tree grant, so it needs
    // DocumentFile.fromFile instead of fromTreeUri - which throws on a
    // non-tree URI. Never happens in a release build.
    val doc =
        if (treeUri.scheme == "file") {
            treeUri.path?.let { path -> DocumentFile.fromFile(File(path)) }
        } else {
            DocumentFile.fromTreeUri(context, treeUri)
        }
    return doc?.name.orEmpty()
}

private fun singleDocumentName(
    context: Context,
    uri: Uri,
): String = DocumentFile.fromSingleUri(context, uri)?.name.orEmpty()
