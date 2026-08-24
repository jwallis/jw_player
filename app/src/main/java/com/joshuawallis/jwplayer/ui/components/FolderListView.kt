package com.joshuawallis.jwplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.joshuawallis.jwplayer.data.DirectoryLister
import com.joshuawallis.jwplayer.data.DirectoryListing

private val ROW_HEIGHT = 48.dp

@Composable
fun FolderListView(
    listing: DirectoryListing,
    showBack: Boolean,
    backLabel: String,
    onBackClick: () -> Unit,
    onFolderClick: (DocumentFile) -> Unit,
    onFileClick: (DocumentFile) -> Unit,
    modifier: Modifier = Modifier,
    highlightedUri: Uri? = null,
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier) {
        if (showBack) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onBackClick)
                        .semantics { contentDescription = "folder $backLabel" }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$backLabel", style = MaterialTheme.typography.titleMedium)
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(listing.folders, key = { it.uri.toString() }) { folder ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onFolderClick(folder) }
                                .semantics { contentDescription = "folder ${folder.name.orEmpty()}" }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(folder.name.orEmpty())
                    }
                }
                items(listing.files, key = { it.uri.toString() }) { file ->
                    val isHighlighted = highlightedUri != null && file.uri == highlightedUri
                    val background = if (isHighlighted) MaterialTheme.colorScheme.onSurface else Color.Transparent
                    val foreground = if (isHighlighted) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(background)
                                .clickable { onFileClick(file) }
                                .semantics { contentDescription = "file ${DirectoryLister.displayName(file)}" }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = foreground)
                        Spacer(Modifier.width(12.dp))
                        Text(DirectoryLister.displayName(file), color = foreground)
                    }
                }
            }

            if (listState.canScrollBackward) {
                ScrollEdgeIndicator(
                    icon = Icons.Filled.KeyboardArrowUp,
                    description = "More folders or files above",
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
            if (listState.canScrollForward) {
                ScrollEdgeIndicator(
                    icon = Icons.Filled.KeyboardArrowDown,
                    description = "More folders or files below",
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

@Composable
private fun ScrollEdgeIndicator(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Icon(icon, contentDescription = description)
    }
}
