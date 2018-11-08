package com.pajato.argus.core.video

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoTest {
    private val repo = createEventStore("build/tmp/argus/video-test")
    private val persister = Persister(repo)

    @Test
    fun exercise_the_Cast_Attribute_property() {
        val firstStar = "Star One"
        val secondStar = "Star Two"
        val castAttribute = Cast(mutableListOf(firstStar, secondStar))
        assertEquals(2, castAttribute.performers.size)
        assertTrue(castAttribute.performers.contains(firstStar))
        assertTrue(castAttribute.performers.contains(secondStar))
        assertEquals(AttributeType.Cast, castAttribute.attrType)
        assertTrue(castAttribute.isEqual(Cast(castAttribute.performers)))
        assertTrue(!castAttribute.isEqual(Cast(mutableListOf("some other star"))))
    }

    @Test
    fun exercise_the_Release_Attribute_property() {
        val timeStamp = 123456L
        val releaseAttribute = Release(timeStamp)
        assertEquals(timeStamp, releaseAttribute.timeStamp)
        assertEquals(AttributeType.Release, releaseAttribute.attrType)
        assertTrue(releaseAttribute.isEqual(Release(releaseAttribute.timeStamp)))
        assertTrue(!releaseAttribute.isEqual(Release( timeStamp + 10L)))
    }

    @Test
    fun exercise_the_Directors_Attribute_property() {
        val firstDirector = "Director One"
        val secondDirector = "Director Two"
        val directorsAttribute = Directors(mutableListOf(firstDirector, secondDirector))
        assertEquals(2, directorsAttribute.directors.size)
        assertTrue(directorsAttribute.directors.contains(firstDirector))
        assertTrue(directorsAttribute.directors.contains(secondDirector))
        assertEquals(AttributeType.Directors, directorsAttribute.attrType)
        assertTrue(directorsAttribute.isEqual(Directors(directorsAttribute.directors)))
        assertTrue(!directorsAttribute.isEqual(Directors(mutableListOf("some other director"))))
    }

    @Test
    fun test_the_Name_Attribute_property() {
        val nameAttribute = Name("testName")
        assertEquals("testName", nameAttribute.name)
        assertEquals(AttributeType.Name, nameAttribute.attrType)
        assertTrue(nameAttribute.isEqual(Name(nameAttribute.name)))
        assertTrue(!nameAttribute.isEqual(Name("some other name")))
    }

    @Test
    fun exercise_the_Provider_Attribute_property() {
        val providerName = "Amazon"
        val providerAttribute = Provider(providerName)
        assertEquals(providerName, providerAttribute.name)
        assertEquals(AttributeType.Provider, providerAttribute.attrType)
        assertTrue(providerAttribute.isEqual(Provider(providerAttribute.name)))
        assertTrue(!providerAttribute.isEqual(Provider("some other provider")))
    }

    @Test
    fun exercise_the_Type_Attribute_property() {
        val typeAttribute = Type(VideoType.Movie)
        assertEquals(VideoType.Movie, typeAttribute.type)
        assertEquals(AttributeType.Type, typeAttribute.attrType)
        assertTrue(typeAttribute.isEqual(Type(typeAttribute.type)))
        assertTrue(!typeAttribute.isEqual(Type(VideoType.TvShow)))

        assertTrue(typeAttribute.updateByReplace)
        typeAttribute.updateAdd(typeAttribute)
        typeAttribute.isEqual(AttributeFactory.create(AttributeType.Name.name, "fred")!!)

    }

    @Test
    fun creating_video_error_does_the_right_thing() {
        val key = ErrorKey.Ok
        val error = VideoError(key)
        assertEquals(key, error.key)
        assertEquals("", error.message)
    }

    @Test
    fun core_video_has_a_correct_id_and_no_attributes() {
        val video = CoreVideo(0)
        assertEquals(0, video.videoId)
        assertEquals(0, video.videoData.size)
    }

    @Test
    fun exercise_the_default_Archive_Event_class() {
        val event = ArchiveEvent()
        assertEquals("Archive", event.key)
        assertEquals("", event.videoId)
        assertEquals("", event.state)
    }

    @Test
    fun exercise_the_Archive_Event_with_an_invalid_id() {
        val event = ArchiveEvent("abc")
        assertEquals("Archive", event.key)
        assertEquals("abc", event.videoId)
        assertEquals("", event.state)
        event.load(mutableMapOf())
    }

    @Test
    fun exercise_the_default_Register_Event_class() {
        val name = "Some Name"
        val event = RegisterEvent("0", name)
        assertEquals("Register", event.key)
        assertEquals("0", event.videoId)
        assertEquals(name, event.videoName)
    }

    @Test
    fun exercise_the_Register_Event_with_an_invalid_id() {
        val name = "Some Name"
        val videoId = "AlphaBetaGamma"
        val event = RegisterEvent(videoId, name)
        assertEquals("Register", event.key)
        assertEquals(videoId, event.videoId)
        assertEquals(name, event.videoName)
        event.load(mutableMapOf())
    }

    @Test
    fun exercise_the_default_Update_append_Event_class() {
        val id = "0"
        val attrType = AttributeType.Cast
        val name = "Keely Hawes"
        val event = UpdateEvent(id, AttributeType.Cast.name, name, UpdateType.Add.name)
        assertEquals("Update", event.key)
        assertEquals(UpdateType.Add.name, event.updateType)
        assertEquals(id, event.videoId)
        assertEquals(attrType.name, event.attributeName)
        assertEquals(name, event.attributeValue)
    }

    @Test
    fun exercise_the_Update_append_Event_with_an_invalid_id() {
        val id = "xyz"
        val attrType = AttributeType.Cast
        val name = "Keely Hawes"
        val event = UpdateEvent(id, AttributeType.Cast.name, name, UpdateType.Add.name)
        assertEquals("Update", event.key)
        assertEquals(UpdateType.Add.name, event.updateType)
        assertEquals(id, event.videoId)
        assertEquals(attrType.name, event.attributeName)
        assertEquals(name, event.attributeValue)
        event.load(mutableMapOf())
    }

    @Test
    fun exercise_the_Update_append_Event_with_an_invalid_attribute() {
        val id = "0"
        val name = "Keely Hawes"
        val attributeName = "InvalidAttributeName"
        val event = UpdateEvent(id, attributeName, name, UpdateType.Add.name)
        event.load(mutableMapOf(0 to CoreVideo(0)))
        assertEquals("Update", event.key)
        assertEquals(UpdateType.Add.name, event.updateType)
        assertEquals(id, event.videoId)
        assertEquals(attributeName, event.attributeName)
        assertEquals(name, event.attributeValue)
    }

    @Test
    fun exercise_the_Update_remove_Event_class() {
        val id = "0"
        val attrType = AttributeType.Cast
        val name = "Keely Hawes"
        val event = UpdateEvent(id, AttributeType.Cast.name, name, UpdateType.Remove.name)
        assertEquals("Update", event.key)
        assertEquals(UpdateType.Remove.name, event.updateType)
        assertEquals(id, event.videoId)
        assertEquals(attrType.name, event.attributeName)
        assertEquals(name, event.attributeValue)
    }

    @Test
    fun exercise_the_Update_removeAll_Event_class() {
        val id = "0"
        val attrType = AttributeType.Cast
        val name = "Keely Hawes"
        val event = UpdateEvent(id, AttributeType.Cast.name, name, UpdateType.RemoveAll.name)
        assertEquals("Update", event.key)
        assertEquals(UpdateType.RemoveAll.name, event.updateType)
        assertEquals(id, event.videoId)
        assertEquals(attrType.name, event.attributeName)
        assertEquals(name, event.attributeValue)
    }

    @Test
    fun when_first_and_second_are_identical_test_that_hasSameEntries_returns_true() {
        val first = mutableListOf("A", "C", "D", "Z")
        val second = mutableListOf("C", "A", "Z", "D")
        assertTrue(hasSameEntries(first, second))
        assertTrue(hasSameEntries(second, first))
    }

    @Test
    fun when_first_and_second_are_not_identical_test_that_hasSameEntries_returns_false() {
        val first = mutableListOf("A", "C", "D", "Z")
        val second = mutableListOf("C", "A", "B", "D")
        assertTrue(!hasSameEntries(first, second))
        assertTrue(!hasSameEntries(second, first))
    }

    @Test
    fun do_an_update_remove_and_removeAll_on_a_core_video_object() {
        val video = CoreVideo(0)
        video.videoData[AttributeType.Provider] = Provider("HBO")
        video.update(mutableSetOf(Provider("")), UpdateType.Remove.name, persister)
        video.update(mutableSetOf(Provider("")), UpdateType.RemoveAll.name, persister)
        video.update(mutableSetOf(Cast(mutableListOf())), UpdateType.Remove.name, persister)
    }
}
