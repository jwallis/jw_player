package com.joshuawallis.jwplayer.ui.screens.main

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun LibraryBrowser(
    rootFolderDoc: DocumentFile?,
    currentFolderDoc: DocumentFile?,
    onFolderChange: (DocumentFile) -> Unit,
    highlightedUri: Uri?,
    onFilePlay: (DocumentFile, List<DocumentFile>) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rootFolderDoc == null || currentFolderDoc == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No music yet. Please choose a folder to get started!")
        }
        return
    }

    val listingCache = remember { ConcurrentHashMap<Uri, DirectoryListing>() }
    val listing =
        remember(currentFolderDoc) {
            listingCache.getOrPut(currentFolderDoc.uri) {
                DirectoryLister.list(currentFolderDoc, AUDIO_EXTENSIONS)
            }
        }
    val parent = remember(currentFolderDoc) { currentFolderDoc.parentFile }

    // Prefetch one level ahead: while browsing this folder, read the contents of each of
    // its subfolders in the background, so drilling into any of them is instant.
    LaunchedEffect(currentFolderDoc) {
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

    FolderListView(
        listing = listing,
        showBack = currentFolderDoc.uri != rootFolderDoc.uri,
        backLabel = currentFolderDoc.name.orEmpty(),
        onBackClick = { parent?.let(onFolderChange) },
        onFolderClick = onFolderChange,
        onFileClick = { file -> onFilePlay(file, listing.files) },
        highlightedUri = highlightedUri,
        modifier = modifier.fillMaxSize(),
    )
}
