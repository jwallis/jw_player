package com.joshuawallis.jwplayer

import com.joshuawallis.jwplayer.ui.navigation.FolderChain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class FolderChainTest {
    private class Node(
        val id: String,
        val parent: Node? = null,
    ) {
        val children = mutableListOf<Node>()

        fun child(id: String): Node = Node(id, this).also { children.add(it) }
    }

    private val root = Node("root")
    private val music = root.child("music")
    private val album = music.child("album")

    private fun ids(node: Node) = FolderChain.idsFromRoot(node, { it.id }, { it.parent })

    private fun resolve(ids: List<String>) = FolderChain.resolve(root, ids, { it.id }, { it.children })

    @Test
    fun `idsFromRoot lists ancestors root first`() {
        assertEquals(listOf("root", "music", "album"), ids(album))
    }

    @Test
    fun `resolve walks back to the saved folder`() {
        assertSame(album, resolve(listOf("root", "music", "album")))
    }

    @Test
    fun `resolve stops at the deepest folder that still exists`() {
        assertSame(music, resolve(listOf("root", "music", "deleted", "album")))
    }

    @Test
    fun `resolve returns root when the chain starts at a different root`() {
        assertSame(root, resolve(listOf("other", "music")))
    }

    @Test
    fun `resolve returns root for an empty chain`() {
        assertSame(root, resolve(emptyList()))
    }
}
