package com.joshuawallis.jwplayer.ui.screens.main

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.documentfile.provider.DocumentFile
import com.joshuawallis.jwplayer.data.AUDIO_EXTENSIONS
import com.joshuawallis.jwplayer.data.DirectoryLister
import com.joshuawallis.jwplayer.data.DirectoryListing
import com.joshuawallis.jwplayer.ui.components.FolderListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** Where a folder's list was scrolled to when the user last left it. */
data class FolderScrollPosition(
    val index: Int,
    val offset: Int,
)

@Composable
fun LibraryBrowser(
    rootFolderDoc: DocumentFile?,
    currentFolderDoc: DocumentFile?,
    onFolderChange: (DocumentFile) -> Unit,
    highlightedUri: Uri?,
    onFilePlay: (DocumentFile, List<DocumentFile>) -> Unit,
    scrollPositions: MutableMap<Uri, FolderScrollPosition>,
    modifier: Modifier = Modifier,
) {
    if (rootFolderDoc == null || currentFolderDoc == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Please choose a root folder ",
                modifier = Modifier.testTag("empty_library_message"),
            )
        }
        return
    }

    // A deleted root folder, or one whose permission was revoked, would otherwise just list as empty.
    val rootAvailable = remember(rootFolderDoc, currentFolderDoc) { rootFolderDoc.exists() && rootFolderDoc.canRead() }
    if (!rootAvailable) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Your root folder is unavailable. Choose it again in Settings.",
                modifier = Modifier.testTag("root_unavailable_message"),
            )
        }
        return
    }

    val listingCache = remember { ConcurrentHashMap<Uri, DirectoryListing>() }
    val cachedListing = remember(currentFolderDoc) { listingCache[currentFolderDoc.uri] }
    var listing by remember(currentFolderDoc) {
        mutableStateOf(
            cachedListing ?: DirectoryLister.list(currentFolderDoc, AUDIO_EXTENSIONS).also {
                listingCache[currentFolderDoc.uri] = it
            },
        )
    }
    val parent = remember(currentFolderDoc) { currentFolderDoc.parentFile }

    LaunchedEffect(currentFolderDoc) {
        // A cached listing may be stale (files added, removed or renamed since it was read):
        // it's shown immediately, then replaced with a fresh listing read in the background.
        if (cachedListing != null) {
            val freshListing = withContext(Dispatchers.IO) { DirectoryLister.list(currentFolderDoc, AUDIO_EXTENSIONS) }
            listingCache[currentFolderDoc.uri] = freshListing
            listing = freshListing
        }

        // Prefetch one level ahead: while browsing this folder, read the contents of each of
        // its subfolders in the background, so drilling into any of them is instant.
        withContext(Dispatchers.IO) {
            listing.folders
                .map { subfolder ->
                    async {
                        listingCache.getOrPut(subfolder.uri) {
                            DirectoryLister.list(subfolder, AUDIO_EXTENSIONS)
                        }
                    }
                }.awaitAll()
        }
    }

    // Each folder gets its own list state, starting where the user last left that folder
    // during this app session (or at the top on its first visit).
    key(currentFolderDoc.uri) {
        val folderUri = currentFolderDoc.uri
        val saved = scrollPositions[folderUri]
        val listState = rememberLazyListState(saved?.index ?: 0, saved?.offset ?: 0)
        DisposableEffect(folderUri) {
            onDispose {
                scrollPositions[folderUri] =
                    FolderScrollPosition(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
            }
        }

        FolderListView(
            listing = listing,
            showBack = currentFolderDoc.uri != rootFolderDoc.uri,
            backLabel = currentFolderDoc.name.orEmpty(),
            onBackClick = { parent?.let(onFolderChange) },
            onFolderClick = onFolderChange,
            onFileClick = { file -> onFilePlay(file, listing.files) },
            highlightedUri = highlightedUri,
            listState = listState,
            modifier = modifier.fillMaxSize(),
        )
    }
}
