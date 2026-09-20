package com.cobraoss.setup

import android.content.Context
import java.io.File
import java.security.MessageDigest

class CoreAssetInstaller(private val context: Context) {
    private val root = File(context.filesDir, "cobra")
    private val bin = File(root, "bin")

    fun install(): File {
        bin.mkdirs()
        context.assets.list("cobra/bin").orEmpty().forEach { name ->
            val destination = File(bin, name)
            context.assets.open("cobra/bin/$name").use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            destination.setExecutable(true, true)
        }
        return root
    }

    fun verifySha256(file: File, expected: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }.equals(expected, true)
    }
}
