package com.cobraoss.terminal

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TerminalSession(context: Context) {
    private val root = File(context.filesDir, "cobra")
    private val home = File(context.filesDir, "terminal").apply { mkdirs() }
    private var process: Process? = null

    suspend fun start(onOutput: suspend (String) -> Unit) = withContext(Dispatchers.IO) {
        val environment = HashMap(System.getenv())
        environment["PATH"] = "${File(root, "bin").absolutePath}:${environment["PATH"].orEmpty()}"
        environment["HOME"] = home.absolutePath
        environment["COBRA_ROOT"] = root.absolutePath
        process = ProcessBuilder("/system/bin/sh").directory(home).redirectErrorStream(true).apply {
            environment().clear(); environment().putAll(environment)
        }.start()
        process!!.inputStream.bufferedReader().useLines { lines -> lines.forEach { line -> onOutput("$line\n") } }
    }

    suspend fun writeLine(command: String) = withContext(Dispatchers.IO) {
        process?.outputStream?.bufferedWriter()?.let { writer -> writer.use { it.write(command); it.newLine(); it.flush() } }
    }

    fun stop() { process?.destroy(); process = null }
}
