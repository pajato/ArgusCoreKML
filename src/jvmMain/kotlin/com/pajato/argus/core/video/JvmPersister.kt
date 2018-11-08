package com.pajato.argus.core.video

import java.io.File

actual fun createEventStore(path: String): EventStore {
    val prefix = "argus"
    val suffix = "txt"
    val defaultName = "$prefix.$suffix"
    val baseFile = File(path)
    fun getRepoFile(): File {
        fun newFile() = File(File(baseFile.parent), baseFile.name).apply { createNewFile() }
        return when {
            baseFile.isFile -> baseFile
            baseFile.isDirectory && File(baseFile, defaultName).isFile -> File(baseFile, defaultName)
            baseFile.isDirectory -> File(baseFile, defaultName).apply { createNewFile() }
            baseFile.parent != null && File(baseFile.parent).isDirectory -> newFile()
            baseFile.parent != null -> {
                File(baseFile.parent).mkdirs()
                newFile()
            }
            else -> throw IllegalArgumentException()
        }
    }

    val repo = try {
        getRepoFile()
    } catch (exc: Exception) {
        throw IllegalArgumentException("$path is not a valid file part!")
    }
    return object : EventStore {
        override val isFile: Boolean = repo.isFile
        override val path: String = repo.path

        override fun appendText(line: String) = repo.appendText(line)

        override fun clear() = repo.writeText("")

        override fun exists() = repo.exists()

        override fun forEachLine(action: (line: String) -> Unit) {
            repo.forEachLine { line -> action(line) }
        }

        override fun size() = repo.readLines().size

        override fun readLines() = repo.readLines()
    }
}
