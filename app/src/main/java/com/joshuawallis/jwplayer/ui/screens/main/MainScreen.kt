package com.joshuawallis.jwplayer.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.joshuawallis.jwplayer.playback.PlaybackViewModel

@Composable
fun MainScreen(
    rootFolderDoc: DocumentFile?,
    currentFolderDoc: DocumentFile?,
    onFolderChange: (DocumentFile) -> Unit,
    playbackViewModel: PlaybackViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by playbackViewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LibraryBrowser(
                rootFolderDoc = rootFolderDoc,
                currentFolderDoc = currentFolderDoc,
                onFolderChange = onFolderChange,
                highlightedUri = uiState.currentFileUri,
                onFilePlay = { file, siblings -> playbackViewModel.playLibraryFile(file, siblings) },
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onSettingsClick,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        MiniPlayer(
            uiState = uiState,
            onTogglePlayPause = playbackViewModel::togglePlayPause,
            onRestartOrPrevious = playbackViewModel::restartOrPrevious,
            onNext = playbackViewModel::next,
            onSeekTo = playbackViewModel::seekTo,
            onBeginHoldSeek = playbackViewModel::beginHoldSeek,
            onHoldSeekTick = playbackViewModel::applyHoldSeekTick,
            onEndHoldSeekNormally = playbackViewModel::endHoldSeekNormally,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
