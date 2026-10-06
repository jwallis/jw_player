package com.joshuawallis.jwplayer.ui.screens.main

import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.joshuawallis.jwplayer.playback.POSITION_TICK_MS
import com.joshuawallis.jwplayer.playback.PlaybackMode
import com.joshuawallis.jwplayer.playback.PlaybackViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    rootFolderDoc: DocumentFile?,
    currentFolderDoc: DocumentFile?,
    onFolderChange: (DocumentFile) -> Unit,
    folderScrollPositions: MutableMap<Uri, FolderScrollPosition>,
    playbackViewModel: PlaybackViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by playbackViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Ticks the elapsed time only while a library track is playing and this screen is visible.
    LaunchedEffect(lifecycleOwner, uiState.mode, uiState.isPlaying) {
        if (uiState.mode != PlaybackMode.LIBRARY) return@LaunchedEffect
        val isPlaying = uiState.isPlaying
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            playbackViewModel.refreshPosition()
            while (isPlaying) {
                delay(POSITION_TICK_MS)
                playbackViewModel.refreshPosition()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LibraryBrowser(
                rootFolderDoc = rootFolderDoc,
                currentFolderDoc = currentFolderDoc,
                onFolderChange = onFolderChange,
                highlightedUri = uiState.currentFileUri,
                onFilePlay = { file, siblings -> playbackViewModel.playLibraryFile(file, siblings) },
                scrollPositions = folderScrollPositions,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onSettingsClick,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .testTag("settings_icon"),
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
