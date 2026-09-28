package org.fcitx.fcitx5.android.input

import android.content.Context
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object LocalRewriter {
    private const val MODEL_NAME = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    private const val MODEL_SIZE = 491400032L
    private val mutex = Mutex()

    init {
        System.loadLibrary("whalekey-rewrite")
    }

    suspend fun rewrite(context: Context, text: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (text.isBlank() || '\u0000' in text) return@withLock null
            val model = try {
                modelFile(context)
            } catch (_: IOException) {
                return@withLock null
            }
            rewriteNative(model.absolutePath, text.encodeToByteArray())
                ?.decodeToString()
                ?.trim()
                ?.takeIf(String::isNotEmpty)
        }
    }

    private fun modelFile(context: Context): File {
        val file = File(context.noBackupFilesDir, MODEL_NAME)
        if (file.length() == MODEL_SIZE) return file

        val temporary = File.createTempFile("whalekey-model-", ".tmp", context.noBackupFilesDir)
        try {
            val copied = context.assets.open("models/$MODEL_NAME").use { input ->
                temporary.outputStream().use { output -> input.copyTo(output) }
            }
            if (copied != MODEL_SIZE || !temporary.renameTo(file)) {
                throw IOException("Could not install bundled rewrite model")
            }
        } finally {
            temporary.delete()
        }
        return file
    }

    private external fun rewriteNative(modelPath: String, text: ByteArray): ByteArray?
}
