package com.pajato.argus.core.video

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class JvmEventStoreTest {

    @Test
    fun forEmptyPathTestThatIllegalArgumentExceptionIsThrown() {
        try {
            createEventStore("")
            fail("Expected illegal argument exception did not get thrown!")
        } catch (exc: Exception) {
            if (exc is IllegalArgumentException) return
            fail("Unexpected exception (${exc.message}) thrown!")
        }
    }

    @Test
    fun whenPathIsDirectoryContainingDefaultFileTestThatEventStoreIsCorrect() {
        try {
            val path = "build/tmp/argus"
            val file = "build/tmp/argus/argus.txt"
            File(file).delete()
            File(file).createNewFile()
            val uut: EventStore = createEventStore(path)
            assertTrue(uut.isFile, "The event store is not a file!")
            assertEquals(file, uut.path, "The actual path is malformed!")
            assertEquals(0, uut.size(), "The event store is not empty!")
        } catch (exc: Exception) {
            fail("Unexpected exception (${exc.message}) thrown!")
        }
    }

    @Test
    fun whenPathIsDirectoryWithoutDefaultFileTestThatDefaultIsCreated() {
        try {
            val path = "build/tmp/argus"
            val file = "build/tmp/argus/argus.txt"
            if (!File(path).isDirectory)
                File(path).mkdirs()
            if (File(file).isFile)
                File(file).delete()
            val uut: EventStore = createEventStore(path)
            assertTrue(uut.isFile, "The event store is not a file!")
            assertEquals(file, uut.path, "The actual path is malformed!")
            assertEquals(0, uut.size(), "The event store is not empty!")
        } catch (exc: Exception) {
            fail("Unexpected exception ($exc) thrown!")
        }
    }

    @Test
    fun whenPathDirectoryDoesExistAndFileDoesNotExistTestThatFileCanBeCreated() {
        try {
            val path = "build/tmp/argus/someName.txt"
            File(path).delete()
            val uut: EventStore = createEventStore(path)
            assertTrue(uut.isFile, "The event store is not a file!")
            assertEquals(path, uut.path, "The actual path (${uut.path} is wrong!")
            assertEquals(0, uut.size(), "The event store is not empty!")
        } catch (exc: Exception) {
            fail("Unexpected exception ($exc) thrown!")
        }
    }

    @Test
    fun whenPathDirectoryDoesNotExistAndFileDoesNotExistTestThatFileCanBeCreated() {
        try {
            val dir = "build/tmp/temp"
            val path = "build/tmp/temp/someName.txt"
            File(dir).delete()
            val uut: EventStore = createEventStore(path)
            assertTrue(uut.isFile, "The event store is not a file!")
            assertEquals(path, uut.path, "The actual path (${uut.path} is wrong!")
            assertEquals(0, uut.size(), "The event store is not empty!")
        } catch (exc: Exception) {
            fail("Unexpected exception ($exc) thrown!")
        }
    }
}
