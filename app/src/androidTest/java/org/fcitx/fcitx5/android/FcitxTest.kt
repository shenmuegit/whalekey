/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android

import android.os.SystemClock
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.fcitx.fcitx5.android.core.Fcitx
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.core.RawConfig
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import timber.log.Timber

class FcitxTest {

    private companion object {

        lateinit var fcitx: Fcitx
        val fcitxEventChannel = Channel<FcitxEvent<*>>(capacity = Channel.CONFLATED)
        val scope = MainScope()

        @BeforeClass
        @JvmStatic
        fun setup() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            fcitx = Fcitx(context)

            // forward to our channel for point to point consuming
            fcitx.eventFlow
                .onEach { fcitxEventChannel.send(it) }
                .launchIn(scope)
            fcitx.start()

            // wait fcitx started
            runBlocking {
                receiveFirst<FcitxEvent.ReadyEvent>()
                fcitx.setEnabledIme(arrayOf("pinyin"))
                fcitx.setGlobalConfig(
                    RawConfig(
                        arrayOf(
                            RawConfig(
                                "Behavior", arrayOf(
                                    RawConfig("ShowInputMethodInformation", false)
                                )
                            )
                        )
                    )
                )
            }
        }

        @AfterClass
        @JvmStatic
        fun cleanup() {
            fcitx.stop()
        }

        private suspend fun sendString(str: String) {
            str.forEach { c ->
                fcitx.sendKey(c)
                delay(50)
            }
        }

        private suspend inline fun <reified T : FcitxEvent<*>> receiveFirst(): T? =
            fcitxEventChannel.receiveAsFlow().mapNotNull { it as? T }.firstOrNull()

        private suspend fun receiveFirstCandidateList() =
            receiveFirst<FcitxEvent.CandidateListEvent>()

        private suspend fun receiveFirstCommitString() =
            receiveFirst<FcitxEvent.CommitStringEvent>()

        private suspend fun receiveFirstPreedit() = receiveFirst<FcitxEvent.ClientPreeditEvent>()

        private suspend fun receiveFirstInputPanelAux() =
            receiveFirst<FcitxEvent.InputPanelEvent>()

    }

    private var enabledIme: List<String> = listOf()

    @Before
    fun saveEnabledIME() = runBlocking {
        enabledIme = fcitx.enabledIme().map { it.uniqueName }
    }

    @After
    fun restoreEnabledIME() = runBlocking {
        fcitx.setEnabledIme(enabledIme.toTypedArray())
    }

    @Test
    fun testPinyin(): Unit = runBlocking {
        fcitx.setEnabledIme(arrayOf("pinyin"))
        sendString("nihaoshijie")
        val expected = "你好世界"
        fcitx.select(0)
        val commitString = receiveFirstCommitString()?.data
        Timber.i("commitString is $commitString")
        Assert.assertEquals(expected, commitString)
        fcitx.reset()
    }

    @Test
    fun testPinyinCandidateBaseline(): Unit = runBlocking {
        fcitx.setEnabledIme(arrayOf("pinyin"))
        fcitx.activate(android.os.Process.myUid(), InstrumentationRegistry.getInstrumentation().targetContext.packageName)
        fcitx.focus()
        fcitx.activateIme("pinyin")
        Assert.assertEquals("pinyin", fcitx.currentIme().uniqueName)
        val cases = listOf(
            Triple("common", "nihao", "你好"),
            Triple("common", "xiexie", "谢谢"),
            Triple("common", "jintian", "今天"),
            Triple("common", "zaoshanghao", "早上好"),
            Triple("homophone", "shijian", "时间"),
            Triple("homophone", "shijie", "世界"),
            Triple("homophone", "zhongyao", "重要"),
            Triple("homophone", "wuli", "物理"),
            Triple("long", "jintiantianqihenhao", "今天天气很好"),
            Triple("long", "woxiangqubeijing", "我想去北京"),
            Triple("name", "beijingdaxue", "北京大学"),
            Triple("name", "qinghuadaxue", "清华大学")
        )
        var top1 = 0
        var top3 = 0
        // Run on a disposable emulator after clearing app data: reset() does not clear user history.
        for ((category, pinyin, expected) in cases) {
            fcitx.reset()
            sendString(pinyin.dropLast(1))
            val start = SystemClock.elapsedRealtimeNanos()
            fcitx.sendKey(pinyin.last())
            val candidates = fcitx.getCandidates(0, 3).map { it.text }
            val elapsedMs = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
            Assert.assertTrue("No candidates for $pinyin", candidates.isNotEmpty())
            if (candidates.first() == expected) top1++
            if (expected in candidates) top3++
            Log.i("WhaleKeyBaseline", "composition,$category,$pinyin,$expected,${candidates.joinToString("|")},candidateReadyMs=$elapsedMs")
        }
        Log.i("WhaleKeyBaseline", "summary,top1=$top1/${cases.size},top3=$top3/${cases.size}")

        for (pinyin in listOf("nihao", "jintian")) {
            fcitx.reset()
            sendString(pinyin)
            val selected = fcitx.getCandidates(0, 1).first().text
            val commit = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeout(3_000) {
                    fcitx.eventFlow.filterIsInstance<FcitxEvent.CommitStringEvent>().first()
                }
            }
            Assert.assertTrue("Could not commit $pinyin", fcitx.select(0))
            Assert.assertEquals(selected, commit.await().data.text)
            val nextWords = fcitx.getCandidates(0, 3).map { it.text }
            Log.i("WhaleKeyBaseline", "next-word,$pinyin,$selected,${nextWords.joinToString("|")}")
        }
        fcitx.reset()
    }

    @Test
    fun testInputPanelStatus(): Unit = runBlocking {
        fcitx.reset()
        Timber.i("after first reset: ${fcitx.isEmpty()}")
        Assert.assertEquals(true, fcitx.isEmpty())
        fcitx.sendKey('a')
        do {
            val list = receiveFirstCandidateList()
        } while (list!!.data.candidates.isNotEmpty())
        Timber.i("after sending 'a': ${fcitx.isEmpty()}")
        Assert.assertEquals(false, fcitx.isEmpty())
        fcitx.reset()
        do {
            val list = receiveFirstCandidateList()
        } while (list!!.data.candidates.isNotEmpty())
        Timber.i("after second reset: ${fcitx.isEmpty()}")
        Assert.assertEquals(true, fcitx.isEmpty())
    }

}
