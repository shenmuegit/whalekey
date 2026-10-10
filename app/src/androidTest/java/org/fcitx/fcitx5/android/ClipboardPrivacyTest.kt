package org.fcitx.fcitx5.android

import android.app.KeyguardManager
import android.content.ClipData
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.SystemClock
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.fcitx.fcitx5.android.data.UserDataManager
import org.fcitx.fcitx5.android.data.clipboard.ClipboardManager
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.clipboard.ClipboardWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.ui.main.MainActivity
import org.fcitx.fcitx5.android.utils.clipboardManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.mechdancer.dependency.DynamicScope
import org.mechdancer.dependency.manager.wrapToUniqueComponent
import org.mechdancer.dependency.plusAssign
import java.util.zip.ZipInputStream

class ClipboardPrivacyTest {
    @Test
    fun rejectsClipboardWindowWhileDeviceLocked() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        assumeTrue("Run with the test device's lock screen showing", keyguard.isKeyguardLocked)
        val manager = InputWindowManager()
        val scope = DynamicScope()
        scope += ContextThemeWrapper(context, R.style.Theme_InputViewTheme).wrapToUniqueComponent()
        scope += manager
        manager.onScopeSetupFinished(scope)
        val clipboard = ClipboardWindow()
        // Reject the window before creating its view or loading any history.
        instrumentation.runOnMainSync { manager.attachWindow(clipboard) }
        assertFalse(manager.isAttached(clipboard))
    }

    @Test
    fun recordsOnlyAfterOptInAndExportsHistory(): Unit = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val pref = AppPrefs.getInstance().clipboard.clipboardListening
        val savedValue = pref.getValue()
        val savedKeyPresent = pref.sharedPreferences.contains(pref.key)
        val marker = "WhaleKey-clipboard-${SystemClock.elapsedRealtime()}"
        var recordedId: Int? = null
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

        fun copy(text: String) {
            instrumentation.runOnMainSync {
                context.clipboardManager.setPrimaryClip(ClipData.newPlainText("", text))
            }
            instrumentation.waitForIdleSync()
        }

        try {
            assertFalse(pref.defaultValue)
            assertFalse("Fresh installs must not enable clipboard history", savedValue)
            ClipboardManager.haveUnpinned()
            SQLiteDatabase.openDatabase(
                context.getDatabasePath("clbdb").path, null, SQLiteDatabase.OPEN_READONLY
            ).use { database ->
                database.rawQuery("SELECT COUNT(*) FROM clipboard WHERE deleted=0", null).use {
                    assertTrue(it.moveToFirst())
                    assertEquals("Run on a fresh test installation", 0, it.getInt(0))
                }
            }
            assertTrue(AppPrefs.getInstance().clipboard.clipboardHistoryLimit.getValue() > 0)
            delay(300)
            val initialCount = ClipboardManager.itemCount
            copy("$marker-before-opt-in")
            delay(750)
            assertEquals(initialCount, ClipboardManager.itemCount)

            instrumentation.runOnMainSync { pref.setValue(true) }
            copy(marker)
            withTimeout(5000) {
                while (ClipboardManager.lastEntry?.text != marker) delay(25)
            }
            recordedId = ClipboardManager.lastEntry!!.id
            withTimeout(5000) {
                while (ClipboardManager.itemCount != initialCount + 1) delay(25)
            }
            assertEquals(marker, ClipboardManager.get(recordedId)!!.text)

            instrumentation.runOnMainSync { pref.setValue(false) }
            copy("$marker-after-opt-out")
            delay(750)
            assertEquals(initialCount + 1, ClipboardManager.itemCount)
            assertEquals(marker, ClipboardManager.lastEntry!!.text)

            val export = context.cacheDir.resolve("clipboard-privacy-export.zip")
            export.outputStream().use { UserDataManager.export(it).getOrThrow() }
            var exportedHistory = false
            val databaseEntries = mutableListOf<String>()
            ZipInputStream(export.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.startsWith("databases/clbdb") && !entry.isDirectory) {
                        databaseEntries.add(entry.name)
                        if (zip.readBytes().toString(Charsets.ISO_8859_1).contains(marker)) {
                            exportedHistory = true
                        }
                    }
                    entry = zip.nextEntry
                }
            }
            assertTrue("Export must contain the opted-in clipboard fixture", exportedHistory)
            Log.i("WhaleKeyClipboard", "defaultOff=true; optInRecorded=true; optOutStopped=true")
            Log.i("WhaleKeyClipboard", "exportIncludesHistory=true; entries=$databaseEntries")
        } finally {
            recordedId?.let { ClipboardManager.delete(it) }
            instrumentation.runOnMainSync {
                if (savedKeyPresent) pref.setValue(savedValue)
                else pref.sharedPreferences.edit().remove(pref.key).commit()
                activity.finish()
            }
        }
    }
}
