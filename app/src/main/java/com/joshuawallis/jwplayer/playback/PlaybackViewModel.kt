package com.joshuawallis.jwplayer.playback

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.joshuawallis.jwplayer.data.DirectoryLister
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PlaybackMode { NONE, LIBRARY, WHITE_NOISE }

enum class SeekDirection { BACKWARD, FORWARD }

data class PlaybackUiState(
    val mode: PlaybackMode = PlaybackMode.NONE,
    val isPlaying: Boolean = false,
    val currentFileUri: Uri? = null,
    val title: String = "",
    val artist: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

private const val HOLD_SEEK_TICK_MS = 30L
private const val HOLD_SEEK_MULTIPLIER = 6
private const val RESTART_THRESHOLD_MS = 3_000L
private const val POSITION_TICK_MS = 200L

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val player = ExoPlayer.Builder(application).build()

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var libraryQueue: List<DocumentFile> = emptyList()
    private var libraryIndex: Int = -1

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && _uiState.value.mode == PlaybackMode.LIBRARY) {
                    advanceToNext(wrap = false)
                }
            }
        })

        viewModelScope.launch {
            while (true) {
                delay(POSITION_TICK_MS)
                if (_uiState.value.mode == PlaybackMode.LIBRARY) {
                    val duration = player.duration.takeIf { it > 0 } ?: 0L
                    _uiState.update { it.copy(positionMs = player.currentPosition, durationMs = duration) }
                }
            }
        }
    }

    private fun refreshPosition() {
        val duration = player.duration.takeIf { it > 0 } ?: 0L
        _uiState.update { it.copy(positionMs = player.currentPosition, durationMs = duration) }
    }

    /** Called when a file is tapped in the Library browser. [siblings] is every playable file in that folder, sorted. */
    fun playLibraryFile(file: DocumentFile, siblings: List<DocumentFile>) {
        libraryQueue = siblings
        val index = siblings.indexOfFirst { it.uri == file.uri }
        if (index == -1) return
        loadLibraryTrack(index)
    }

    fun togglePlayPause() {
        when (_uiState.value.mode) {
            PlaybackMode.LIBRARY -> if (player.isPlaying) player.pause() else player.play()
            PlaybackMode.WHITE_NOISE, PlaybackMode.NONE -> {
                if (libraryIndex >= 0) loadLibraryTrack(libraryIndex)
            }
        }
    }

    /** Button 3a: restart current track, or jump to the previous file if within the first 3s. */
    fun restartOrPrevious() {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return
        if (player.currentPosition < RESTART_THRESHOLD_MS && libraryIndex > 0) {
            loadLibraryTrack(libraryIndex - 1)
        } else {
            player.seekTo(0)
            player.play()
            refreshPosition()
        }
    }

    /** Button 3e: next file, wrapping to the first file if currently on the last. */
    fun next() {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return
        advanceToNext(wrap = true)
    }

    fun beginHoldSeek() {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return
        player.volume = 0f
        player.pause()
    }

    /** Advances the seek position by [elapsedRealtimeMs] x 3 in [direction]. Returns true if a track boundary was hit. */
    fun applyHoldSeekTick(direction: SeekDirection, elapsedRealtimeMs: Long): Boolean {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return true
        val duration = player.duration.takeIf { it > 0 } ?: return false
        val delta = elapsedRealtimeMs * HOLD_SEEK_MULTIPLIER
        val current = player.currentPosition
        return when (direction) {
            SeekDirection.BACKWARD -> {
                val target = current - delta
                if (target <= 0) {
                    player.seekTo(0)
                    player.volume = 1f
                    player.play()
                    refreshPosition()
                    true
                } else {
                    player.seekTo(target)
                    refreshPosition()
                    false
                }
            }
            SeekDirection.FORWARD -> {
                val target = current + delta
                if (target >= duration) {
                    player.volume = 1f
                    advanceToNext(wrap = true)
                    true
                } else {
                    player.seekTo(target)
                    refreshPosition()
                    false
                }
            }
        }
    }

    /** Called on release of a hold-seek gesture that did not already hit a track boundary. */
    fun endHoldSeekNormally() {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return
        player.volume = 1f
        player.play()
        refreshPosition()
    }

    fun seekTo(positionMs: Long) {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return
        player.seekTo(positionMs)
        refreshPosition()
    }

    fun playWhiteNoise(uri: Uri) {
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = 1f
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
        _uiState.update { it.copy(mode = PlaybackMode.WHITE_NOISE) }
    }

    fun pauseWhiteNoise() {
        if (_uiState.value.mode != PlaybackMode.WHITE_NOISE) return
        player.pause()
        _uiState.update { it.copy(mode = PlaybackMode.NONE) }
    }

    fun toggleWhiteNoise(uri: Uri?) {
        if (uri == null) return
        if (_uiState.value.mode == PlaybackMode.WHITE_NOISE && player.isPlaying) {
            pauseWhiteNoise()
        } else {
            playWhiteNoise(uri)
        }
    }

    private fun advanceToNext(wrap: Boolean) {
        if (libraryQueue.isEmpty()) return
        val nextIndex = libraryIndex + 1
        when {
            nextIndex < libraryQueue.size -> loadLibraryTrack(nextIndex)
            wrap -> loadLibraryTrack(0)
            else -> {
                player.stop()
                _uiState.update { it.copy(mode = PlaybackMode.NONE) }
                refreshPosition()
            }
        }
    }

    private fun loadLibraryTrack(index: Int) {
        val file = libraryQueue.getOrNull(index) ?: return
        libraryIndex = index
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.volume = 1f
        player.setMediaItem(MediaItem.fromUri(file.uri))
        player.prepare()
        player.play()

        val artist = Metadata.readArtist(getApplication(), file.uri)
        val title = DirectoryLister.displayName(file)
        _uiState.update {
            it.copy(
                mode = PlaybackMode.LIBRARY,
                currentFileUri = file.uri,
                title = title,
                artist = artist
            )
        }
        refreshPosition()
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
