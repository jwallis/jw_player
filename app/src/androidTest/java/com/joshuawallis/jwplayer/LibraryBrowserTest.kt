package com.joshuawallis.jwplayer

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.joshuawallis.jwplayer.ui.screens.main.LibraryBrowser
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryBrowserTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsFriendlyPlaceholderWhenNoRootFolderSelected() {
        composeTestRule.setContent {
            LibraryBrowser(
                rootFolderDoc = null,
                currentFolderDoc = null,
                onFolderChange = {},
                highlightedUri = null,
                onFilePlay = { _, _ -> },
            )
        }

        composeTestRule.onNodeWithText("Please choose a root folder to get started so that I can show some songs!!").assertExists()
    }
}
