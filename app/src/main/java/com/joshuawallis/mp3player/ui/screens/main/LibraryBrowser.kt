package com.joshuawallis.mp3player.ui.screens.main

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.documentfile.provider.DocumentFile
import com.joshuawallis.mp3player.data.AUDIO_EXTENSIONS
import com.joshuawallis.mp3player.data.DirectoryLister
import com.joshuawallis.mp3player.ui.components.FolderListView

@Composable
fun LibraryBrowser(
    rootFolderDoc: DocumentFile?,
    currentFolderDoc: DocumentFile?,
    onFolderChange: (DocumentFile) -> Unit,
    highlightedUri: Uri?,
    onFilePlay: (DocumentFile, List<DocumentFile>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (rootFolderDoc == null || currentFolderDoc == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("set the root folder")
        }
        return
    }

    val listing = remember(currentFolderDoc) {
        DirectoryLister.list(currentFolderDoc, AUDIO_EXTENSIONS)
    }
    val parent = remember(currentFolderDoc) { currentFolderDoc.parentFile }

    FolderListView(
        listing = listing,
        showBack = currentFolderDoc.uri != rootFolderDoc.uri,
        backLabel = currentFolderDoc.name.orEmpty(),
        onBackClick = { parent?.let(onFolderChange) },
        onFolderClick = onFolderChange,
        onFileClick = { file -> onFilePlay(file, listing.files) },
        highlightedUri = highlightedUri,
        modifier = modifier.fillMaxSize()
    )
}
