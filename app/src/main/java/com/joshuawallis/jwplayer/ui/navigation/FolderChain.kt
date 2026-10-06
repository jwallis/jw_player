package com.joshuawallis.jwplayer.ui.navigation

/**
 * Saves and restores a position in a folder tree as the chain of folder IDs from
 * the root down. A folder rebuilt from a bare ID has no parent, so restoring
 * walks down from the root to rebuild a folder whose parent chain still works.
 */
object FolderChain {
    /** IDs of [folder] and each of its ancestors, root first. */
    fun <T> idsFromRoot(
        folder: T,
        idOf: (T) -> String,
        parentOf: (T) -> T?,
    ): List<String> {
        val ids = mutableListOf<String>()
        var current: T? = folder
        while (current != null) {
            ids.add(idOf(current))
            current = parentOf(current)
        }
        return ids.reversed()
    }

    /**
     * Walks down from [root] following [ids] (root's own ID first), returning the
     * deepest folder found. Returns [root] if the first ID isn't the root's.
     */
    fun <T> resolve(
        root: T,
        ids: List<String>,
        idOf: (T) -> String,
        childrenOf: (T) -> List<T>,
    ): T {
        if (ids.firstOrNull() != idOf(root)) return root
        var current = root
        for (id in ids.drop(1)) {
            current = childrenOf(current).firstOrNull { idOf(it) == id } ?: return current
        }
        return current
    }
}
