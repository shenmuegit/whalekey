package org.fcitx.fcitx5.android

import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.fcitx.fcitx5.android.input.LocalRewriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocalRewriterTest {
    @Test
    fun rewritesChineseInAirplaneMode() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(1, Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON))

        val original = "由于明天我有事情的原因，所以这个会可能我就去不了。"
        val rewritten = LocalRewriter.rewrite(context, original)
        assertNotNull(rewritten)
        assertNotEquals(original, rewritten)
        assertEquals(491400032L, context.noBackupFilesDir.resolve("qwen2.5-0.5b-instruct-q4_k_m.gguf").length())
    }
}
