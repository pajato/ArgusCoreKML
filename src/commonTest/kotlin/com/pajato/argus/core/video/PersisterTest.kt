package com.pajato.argus.core.video

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class PersisterTest {
    private val repo = createEventStore("build/tmp/argus/persister-test")
    private val uut = Persister(repo)

    @Test
    fun add_text_to_the_event_store_clear_it_and_verify() {
        repo.clear()
        repo.appendText("testing line one line\ntesting line two\n")
        assertEquals(2, repo.readLines().size)
        uut.clear()
        assertEquals(0, repo.readLines().size)
    }

    @Test
    fun load_a_non_archived_video() {
        fun setup() {
            repo.clear()
            val events = "Register 0 MI5\nArchive 0 false\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
            assertEquals(2, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertFalse(videoMap[0]!!.archived)
        uut.archive(videoMap[0]!!)
    }

    @Test
    fun load_an_archived_video() {
        fun setup() {
            repo.clear()
            val events = "Register 0 MI5\nArchive 0 true\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0L)
            assertEquals(2, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertTrue(videoMap[0]!!.archived)
        uut.archive(videoMap[0]!!)
    }

    @Test
    fun load_a_non_archived_video_for_code_coverage() {
        fun setup() {
            repo.clear()
            val events = "Register 0 MI5\nArchive 1 false\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0L)
            assertEquals(2, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertFalse(videoMap[0]!!.archived)
        uut.archive(videoMap[0]!!)
    }

    @Test
    fun load_an_archived_video_for_code_coverage() {
        fun setup() {
            repo.clear()
            val events = "Register 0 MI5\nArchive 1 true\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0L)
            assertEquals(2, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertFalse(videoMap[0]!!.archived)
        uut.archive(videoMap[0]!!)
    }

    @Test
    fun testRegisterEvent() {
        val id = 0
        val name = "Persister Test Video"
        fun setup() {
            repo.clear()
            val event = "Register $id $name\n"
            repo.appendText(event)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
            assertEquals(1, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertEquals(1, videoMap.size)
        assertEquals(1, videoMap[0]!!.videoData.size)
        assertEquals(id, videoMap[0]!!.videoId)
        assertTrue((videoMap[0]!!.videoData[AttributeType.Name]!!.isEqual(Name(name))))
    }

    @Test
    fun testUpdateAddEvent() {
        fun setup() {
            repo.clear()
            val events = "Register 0 MI5\nUpdate 0 Add Cast Keely Hawes\nUpdate 0 Add Cast Matthew MacFadyen\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
            assertEquals(3, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertEquals(1, videoMap.size)
        assertEquals(2, videoMap[0]!!.videoData.size)
        val attribute = videoMap[0]!!.videoData[AttributeType.Cast]
        if (attribute is Cast)
            assertEquals(2, attribute.performers.size)
        else
            fail("The cast attribute does not exist!")
        repo.clear()
    }

    @Test
    fun testUpdateRemoveEvent() {
        fun setup() {
            repo.clear()
            val events = "Register 0 North By Northwest\nUpdate 0 Add Directors Alfred Hitchcock\n" +
                    "Update 0 Add Directors His Assistant\nUpdate 0 Add Directors Steven Spielberg\n" +
                    "Update 0 Remove Directors Alfred Hitchcock\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
            assertEquals(5, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertEquals(1, videoMap.size)
        assertEquals(2, videoMap[0]!!.videoData.size)
        val attribute = videoMap[0]!!.videoData[AttributeType.Directors]
        if (attribute is Directors)
            assertEquals(2, attribute.directors.size)
        else
            fail("The directors attribute does not exist!")
        repo.clear()
    }

    @Test
    fun testUpdateRemoveAllEvent() {
        fun setup() {
            repo.clear()
            val events = "Register 0 North By Northwest\nUpdate 0 Add Directors Alfred Hitchcock\n" +
                    "Update 0 RemoveAll Directors Dummy Director\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
            assertEquals(3, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertEquals(1, videoMap.size)
        assertEquals(1, videoMap[0]!!.videoData.size)
        repo.clear()
    }

    @Test
    fun when_there_are_invalid_events_in_the_repo_load_should_ignore_them_quietly() {
        fun setup() {
            repo.clear()
            val events = "\nRegister\nRegister invalidId\nRegister 0\nRegister 0 fred\nInvalid 0 rest" +
                    "Update 0 Odd\nUpdate 0 Add \nUpdate 0 Add Name\nArchive 0\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
            assertEquals(9, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertEquals(1, videoMap.size)
    }

    @Test
    fun update_a_fully_loaded_video_that_has_all_and_valid_attributes() {
        fun setup() {
            repo.clear()
            val events = "Register 0 A fully loaded video\n" +
                    "Update 0 Add Cast Some Star\n" +
                    "Update 0 Add Directors Some Director\n" +
                    "Update 0 Add Name The New Video Title\n" +
                    "Update 0 Add Provider Netflix\n" +
                    "Update 0 Add Release 10\n" +
                    "Update 0 Add Type Movie\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
            assertEquals(7, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertEquals(1, videoMap.size)
    }


    @Test
    fun test_that_the_default_case_for_event_type_CoverageDefault_does_nothing() {
        fun setup() {
            repo.clear()
            val events = "Register 0 A fully loaded video\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
            assertEquals(1, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertEquals(1, videoMap.size)
    }

    @Test
    fun test_that_an_invalid_id_causes_an_abort() {
        fun setup() {
            repo.clear()
            val events = "Register 0 A fully loaded video\n" +
                    "Update 23 Add Name dummy name for unregistered video\n"
            repo.appendText(events)
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
            assertEquals(2, repo.size())
        }

        setup()
        val videoMap = uut.load()
        assertEquals(1, videoMap.size)
    }
}
