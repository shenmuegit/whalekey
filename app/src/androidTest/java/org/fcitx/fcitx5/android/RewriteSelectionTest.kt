/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */
package org.fcitx.fcitx5.android

import androidx.test.platform.app.InstrumentationRegistry
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RewriteSelectionTest {
    private val generation = FcitxInputMethodService::class.java
        .getDeclaredField("rewriteGeneration").apply { isAccessible = true }

    @Test
    fun selectionRoundTripDoesNotRestoreRewriteValidity() = onMainThread {
        val service = FcitxInputMethodService()
        service.onUpdateSelection(-1, -1, 2, 27, -1, -1)
        val previewGeneration = generation.getInt(service)

        service.onUpdateSelection(2, 27, 29, 54, -1, -1)
        service.onUpdateSelection(29, 54, 2, 27, -1, -1)

        assertEquals(2, service.currentInputSelection.start)
        assertEquals(27, service.currentInputSelection.end)
        assertNotEquals("Returning to A must not restore the preview's generation",
            previewGeneration, generation.getInt(service))
    }

    @Test
    fun movingEitherSelectionEndpointInvalidatesRewrite() = onMainThread {
        for ((start, end) in listOf(3 to 27, 2 to 28)) {
            val service = FcitxInputMethodService()
            service.onUpdateSelection(-1, -1, 2, 27, -1, -1)
            val previewGeneration = generation.getInt(service)

            service.onUpdateSelection(2, 27, start, end, -1, -1)

            assertNotEquals("Moving either endpoint must invalidate the existing preview",
                previewGeneration, generation.getInt(service))
        }
    }

    @Test
    fun unchangedSelectionPreservesRewriteValidity() = onMainThread {
        val service = FcitxInputMethodService()
        service.onUpdateSelection(-1, -1, 2, 27, -1, -1)
        val previewGeneration = generation.getInt(service)

        service.onUpdateSelection(2, 27, 2, 27, -1, -1)
        service.onUpdateSelection(2, 27, 2, 27, 2, 27)

        assertEquals(previewGeneration, generation.getInt(service))
    }

    private fun onMainThread(block: () -> Unit) {
        var result: Result<Unit>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = runCatching(block)
        }
        result!!.getOrThrow()
    }
}
