package com.joshuawallis.jwplayer.playback

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.joshuawallis.jwplayer.data.DirectoryLister
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PlaybackMode { NONE, LIBRARY, WHITE_NOISE }

enum class SeekDirection { BACKWARD, FORWARD }

data class PlaybackUiState(
    val mode: PlaybackMode = PlaybackMode.NONE,
    val isPlaying: Boolean = false,
    val currentFileUri: Uri? = null,
    val title: String = "",
    val artist: String = "",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

private const val WHITE_NOISE_TITLE = "White Noise"

/** White noise has no track identity or meaningful position: show its name, an empty seek bar, and no highlighted library row. */
private fun PlaybackUiState.showingWhiteNoise(): PlaybackUiState =
    copy(
        mode = PlaybackMode.WHITE_NOISE,
        title = WHITE_NOISE_TITLE,
        artist = "",
        currentFileUri = null,
        positionMs = 0L,
        durationMs = 0L,
    )

private const val HOLD_SEEK_TICK_MS = 30L
private const val HOLD_SEEK_MULTIPLIER = 10
private const val RESTART_THRESHOLD_MS = 3_000L
private const val POSITION_TICK_MS = 200L

class PlaybackViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private var player: Player? = null

    private val controllerFuture =
        MediaController
            .Builder(
                application,
                SessionToken(application, ComponentName(application, PlaybackService::class.java)),
            ).buildAsync()

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var libraryQueue: List<DocumentFile> = emptyList()
    private var libraryIndex: Int = -1
    private var libraryLoadJob: Job? = null
    private var artistLoadJob: Job? = null
    private var artistLoadMediaId: String? = null

    init {
        controllerFuture.addListener(
            {
                player = controllerFuture.get()
                player?.addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            syncFromPlayer()
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            val isLibraryItem = MediaIds.libraryUriString(player?.currentMediaItem?.mediaId) != null
                            if (playbackState == Player.STATE_ENDED && isLibraryItem) {
                                player?.stop()
                                _uiState.update { it.copy(mode = PlaybackMode.NONE) }
                                refreshPosition()
                            } else {
                                syncFromPlayer()
                            }
                        }

                        override fun onMediaItemTransition(
                            mediaItem: MediaItem?,
                            reason: Int,
                        ) {
                            syncFromPlayer()
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            recoverFromPlayerError()
                        }
                    },
                )
                syncFromPlayer()
            },
            MoreExecutors.directExecutor(),
        )

        viewModelScope.launch {
            while (true) {
                delay(POSITION_TICK_MS)
                if (_uiState.value.mode == PlaybackMode.LIBRARY) {
                    refreshPosition()
                }
            }
        }
    }

    /**
     * Derives mode and the current track from whatever the player is actually doing. Runs on connect and on every
     * isPlaying/state/item change, so playback started outside the app (headset, notification) is reflected too.
     */
    private fun syncFromPlayer() {
        val p = player ?: return
        val item = p.currentMediaItem
        val mode = MediaIds.modeFor(item?.mediaId, p.playbackState)
        _uiState.update { it.copy(mode = mode, isPlaying = p.isPlaying) }
        if (mode == PlaybackMode.WHITE_NOISE) {
            _uiState.update { it.showingWhiteNoise() }
            return
        }
        val uriString = MediaIds.libraryUriString(item?.mediaId)
        if (mode != PlaybackMode.LIBRARY || item == null || uriString == null) return
        libraryIndex = p.currentMediaItemIndex
        _uiState.update {
            it.copy(
                currentFileUri = uriString.toUri(),
                title =
                    item.mediaMetadata.title
                        ?.toString()
                        .orEmpty(),
                artist =
                    item.mediaMetadata.artist
                        ?.toString()
                        .orEmpty(),
            )
        }
        refreshPosition()
        loadArtistIfMissing(item, uriString.toUri())
    }

    /**
     * Library items start playing with a title only, so playback never waits on reading tags. Once an item becomes
     * current, this reads its artist in the background and swaps in a copy of the item carrying it, so the
     * mini-player, notification and Bluetooth display all pick it up. The swapped item has the same URI, so the
     * player updates it in place without interrupting playback.
     */
    private fun loadArtistIfMissing(
        item: MediaItem,
        uri: Uri,
    ) {
        if (item.mediaMetadata.artist != null || artistLoadMediaId == item.mediaId) return
        artistLoadMediaId = item.mediaId
        artistLoadJob?.cancel()
        artistLoadJob =
            viewModelScope.launch {
                try {
                    val artist = withContext(Dispatchers.IO) { Metadata.readArtist(getApplication(), uri) }
                    val p = player ?: return@launch
                    if (p.currentMediaItem?.mediaId != item.mediaId) return@launch
                    _uiState.update { if (it.currentFileUri == uri) it.copy(artist = artist) else it }
                    p.replaceMediaItem(
                        p.currentMediaItemIndex,
                        libraryMediaItem(
                            uri,
                            item.mediaMetadata.title
                                ?.toString()
                                .orEmpty(),
                            artist,
                        ),
                    )
                } finally {
                    if (artistLoadMediaId == item.mediaId) artistLoadMediaId = null
                }
            }
    }

    private fun libraryMediaItem(
        uri: Uri,
        title: String,
        artist: String?,
    ): MediaItem =
        MediaItem
            .Builder()
            .setMediaId(MediaIds.forLibraryFile(uri.toString()))
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build(),
            ).build()

    /**
     * An errored player sits idle and ignores play(). For a library track, skip to the next one (re-preparing the
     * player); with no next track, or for white noise, stop and clear the mini-player. Either way, tell the user.
     */
    private fun recoverFromPlayerError() {
        val p = player ?: return
        val failedTitle = _uiState.value.title
        val isLibraryItem = MediaIds.libraryUriString(p.currentMediaItem?.mediaId) != null
        if (isLibraryItem && p.hasNextMediaItem()) {
            p.seekToNextMediaItem()
            p.prepare()
            p.play()
        } else {
            p.stop()
            _uiState.value = PlaybackUiState()
        }
        val message = if (failedTitle.isNotBlank()) "Couldn't play $failedTitle" else "Couldn't play this file"
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }

    private fun refreshPosition() {
        val duration = player?.duration?.takeIf { it > 0 } ?: 0L
        _uiState.update { it.copy(positionMs = player?.currentPosition ?: 0L, durationMs = duration) }
    }

    /** Called when a file is tapped in the Library browser. [siblings] is every playable file in that folder, sorted. */
    fun playLibraryFile(
        file: DocumentFile,
        siblings: List<DocumentFile>,
    ) {
        val index = siblings.indexOfFirst { it.uri == file.uri }
        if (index == -1) return
        startLibraryPlayback(siblings, index)
    }

    fun togglePlayPause() {
        when (_uiState.value.mode) {
            PlaybackMode.LIBRARY, PlaybackMode.WHITE_NOISE -> if (player?.isPlaying == true) player?.pause() else player?.play()
            PlaybackMode.NONE -> {
                if (libraryIndex in libraryQueue.indices) startLibraryPlayback(libraryQueue, libraryIndex)
            }
        }
    }

    /** Button 3a: restart current track, or jump to the previous file if within the first 3s. */
    fun restartOrPrevious() {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return
        val index = player?.currentMediaItemIndex ?: return
        val position = player?.currentPosition ?: 0L
        if (position < RESTART_THRESHOLD_MS && index > 0) {
            player?.seekTo(index - 1, 0)
            player?.play()
        } else {
            player?.seekTo(0)
            player?.play()
            refreshPosition()
        }
    }

    /** Button 3e: next file. Does not wrap when already on the last file. */
    fun next() {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return
        player?.seekToNextMediaItem()
        player?.play()
    }

    fun beginHoldSeek() {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return
        player?.volume = 0f
        player?.pause()
    }

    /** Advances the seek position by [elapsedRealtimeMs] x 3 in [direction]. Returns true if a track boundary was hit. */
    fun applyHoldSeekTick(
        direction: SeekDirection,
        elapsedRealtimeMs: Long,
    ): Boolean {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return true
        val duration = player?.duration?.takeIf { it > 0 } ?: return false
        val delta = elapsedRealtimeMs * HOLD_SEEK_MULTIPLIER
        val current = player?.currentPosition ?: return false
        return when (direction) {
            SeekDirection.BACKWARD -> {
                val target = current - delta
                if (target <= 0) {
                    player?.seekTo(0)
                    player?.volume = 1f
                    player?.play()
                    refreshPosition()
                    true
                } else {
                    player?.seekTo(target)
                    refreshPosition()
                    false
                }
            }
            SeekDirection.FORWARD -> {
                val target = current + delta
                if (target >= duration) {
                    player?.volume = 1f
                    player?.seekToNextMediaItem()
                    player?.play()
                    true
                } else {
                    player?.seekTo(target)
                    refreshPosition()
                    false
                }
            }
        }
    }

    /** Called on release of a hold-seek gesture that did not already hit a track boundary. */
    fun endHoldSeekNormally() {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return
        player?.volume = 1f
        player?.play()
        refreshPosition()
    }

    fun seekTo(positionMs: Long) {
        if (_uiState.value.mode != PlaybackMode.LIBRARY) return
        player?.seekTo(positionMs)
        refreshPosition()
    }

    fun playWhiteNoise(uri: Uri) {
        player?.repeatMode = Player.REPEAT_MODE_ONE
        player?.volume = 1f
        player?.setMediaItem(
            MediaItem
                .Builder()
                .setMediaId(MediaIds.WHITE_NOISE)
                .setUri(uri)
                .build(),
        )
        player?.prepare()
        player?.play()
        _uiState.update { it.showingWhiteNoise() }
    }

    fun pauseWhiteNoise() {
        if (_uiState.value.mode != PlaybackMode.WHITE_NOISE) return
        player?.stop()
        _uiState.update { it.copy(mode = PlaybackMode.NONE) }
    }

    fun toggleWhiteNoise(uri: Uri?) {
        if (uri == null) return
        if (_uiState.value.mode == PlaybackMode.WHITE_NOISE && player?.isPlaying == true) {
            pauseWhiteNoise()
        } else {
            playWhiteNoise(uri)
        }
    }

    private fun startLibraryPlayback(
        queue: List<DocumentFile>,
        startIndex: Int,
    ) {
        libraryLoadJob?.cancel()
        libraryLoadJob =
            viewModelScope.launch {
                // Display names only (artist is read lazily once a track becomes current), so playback starts at once.
                val mediaItems =
                    withContext(Dispatchers.IO) {
                        queue.map { file -> libraryMediaItem(file.uri, DirectoryLister.displayName(file), artist = null) }
                    }
                libraryQueue = queue
                libraryIndex = startIndex
                player?.repeatMode = Player.REPEAT_MODE_OFF
                player?.volume = 1f
                player?.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
                player?.prepare()
                player?.play()
                val startMetadata = mediaItems.getOrNull(startIndex)?.mediaMetadata
                _uiState.update {
                    it.copy(
                        mode = PlaybackMode.LIBRARY,
                        currentFileUri = queue.getOrNull(startIndex)?.uri,
                        title = startMetadata?.title?.toString() ?: "",
                        artist = startMetadata?.artist?.toString() ?: "",
                    )
                }
                refreshPosition()
            }
    }

    override fun onCleared() {
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
