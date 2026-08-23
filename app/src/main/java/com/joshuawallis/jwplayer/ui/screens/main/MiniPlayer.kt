package com.joshuawallis.mp3player.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joshuawallis.mp3player.playback.PlaybackUiState
import com.joshuawallis.mp3player.playback.SeekDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun MiniPlayer(
    uiState: PlaybackUiState,
    onTogglePlayPause: () -> Unit,
    onRestartOrPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onBeginHoldSeek: () -> Unit,
    onHoldSeekTick: (SeekDirection, Long) -> Boolean,
    onEndHoldSeekNormally: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        val titleLine = if (uiState.artist.isNotBlank()) "${uiState.artist} - ${uiState.title}" else uiState.title
        Text(
            text = titleLine,
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = formatTime(uiState.positionMs),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        val progress = if (uiState.durationMs > 0) {
            (uiState.positionMs.toFloat() / uiState.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        SeekBar(
            progress = progress,
            onSeek = { fraction -> onSeekTo((fraction * uiState.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRestartOrPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Restart or previous track")
            }

            HoldSeekButton(
                icon = Icons.Filled.FastRewind,
                contentDescription = "Seek backward",
                direction = SeekDirection.BACKWARD,
                onBegin = onBeginHoldSeek,
                onTick = onHoldSeekTick,
                onEndNormally = onEndHoldSeekNormally
            )

            IconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play or pause"
                )
            }

            HoldSeekButton(
                icon = Icons.Filled.FastForward,
                contentDescription = "Seek forward",
                direction = SeekDirection.FORWARD,
                onBegin = onBeginHoldSeek,
                onTick = onHoldSeekTick,
                onEndNormally = onEndHoldSeekNormally
            )

            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next track")
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun SeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val displayProgress = (dragProgress ?: progress).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDrag = { change, _ ->
                        dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragProgress?.let(onSeek)
                        dragProgress = null
                    },
                    onDragCancel = { dragProgress = null }
                )
            }
    ) {
        val trackWidth = maxWidth
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = trackWidth * displayProgress - 6.dp)
                .size(12.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

@Composable
private fun HoldSeekButton(
    icon: ImageVector,
    contentDescription: String,
    direction: SeekDirection,
    onBegin: () -> Unit,
    onTick: (SeekDirection, Long) -> Boolean,
    onEndNormally: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(direction) {
                detectTapGestures(
                    onPress = {
                        var endedAtEdge = false
                        val job = scope.launch {
                            onBegin()
                            var lastTick = System.currentTimeMillis()
                            while (isActive) {
                                delay(30)
                                val now = System.currentTimeMillis()
                                val elapsed = now - lastTick
                                lastTick = now
                                if (onTick(direction, elapsed)) {
                                    endedAtEdge = true
                                    break
                                }
                            }
                        }
                        try {
                            awaitRelease()
                        } finally {
                            job.cancel()
                            if (!endedAtEdge) onEndNormally()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}
