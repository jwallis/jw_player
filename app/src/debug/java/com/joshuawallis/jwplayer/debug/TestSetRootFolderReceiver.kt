package com.joshuawallis.jwplayer.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.joshuawallis.jwplayer.data.SettingsRepository
import java.io.File

/**
 * Automation backdoor for setting the root folder directly, bypassing the
 * real Storage Access Framework picker - test_cases.md already marks that
 * picker "not automatable" (flaky across OEMs/Android versions), so
 * automation never drives it. Lives in the debug source set only; never
 * compiled into a release build. Driven via:
 * `adb shell am broadcast -a com.joshuawallis.jwplayer.TEST_SET_ROOT_FOLDER
 * -e path /sdcard/testdata --receiver-include-background`
 */
class TestSetRootFolderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val path = intent.getStringExtra(EXTRA_PATH) ?: return
        SettingsRepository(context).setRootFolderUri(Uri.fromFile(File(path)))
    }

    companion object {
        const val ACTION = "com.joshuawallis.jwplayer.TEST_SET_ROOT_FOLDER"
        const val EXTRA_PATH = "path"
    }
}
