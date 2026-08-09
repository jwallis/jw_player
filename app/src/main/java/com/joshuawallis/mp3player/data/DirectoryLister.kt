package com.joshuawallis.mp3player.data

import androidx.documentfile.provider.DocumentFile

val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "wav")

data class DirectoryListing(
    val folders: List<DocumentFile>,
    val files: List<DocumentFile>
)

object DirectoryLister {

    fun list(dir: DocumentFile, extensionFilter: Set<String>?, includeFiles: Boolean = true): DirectoryListing {
        val children = dir.listFiles()

        val folders = children
            .filter { it.isDirectory && !it.name.orEmpty().startsWith(".") }
            .sortedBy { it.name.orEmpty().lowercase() }

        val files = if (includeFiles) {
            children
                .filter { it.isFile && !it.name.orEmpty().startsWith(".") && matchesExtension(it, extensionFilter) }
                .sortedBy { it.name.orEmpty().lowercase() }
        } else {
            emptyList()
        }

        return DirectoryListing(folders, files)
    }

    fun displayName(file: DocumentFile): String = titleFromFileName(file.name.orEmpty())

    fun titleFromFileName(fileName: String): String = fileName.substringBeforeLast('.')

    private fun matchesExtension(file: DocumentFile, extensionFilter: Set<String>?): Boolean {
        if (extensionFilter == null) return true
        val extension = file.name.orEmpty().substringAfterLast('.', "").lowercase()
        return extension in extensionFilter
    }
}
