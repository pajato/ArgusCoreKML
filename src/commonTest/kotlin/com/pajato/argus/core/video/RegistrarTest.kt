package com.pajato.argus.core.video

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrarTest {
    private val repo = createEventStore("build/tmp/argus/registrar-test")
    private val registrar = Registrar(repo)
    private val interactor = VideoInteractor(registrar)

    @BeforeTest
    fun init() {
        registrar.reset()
        repo.forEachLine {
            fail("The persistence store is not empty after reset!")
        }
    }

    @Test
    fun when_a_video_is_registered_and_archived_the_repo_should_have_one_more_line() {
        val video = interactor.register("Video One Register and Archive Test")
        assertEquals(1, repo.readLines().size)
        val coreVideo = video as CoreVideo
        interactor.archive(coreVideo.videoId, true)
        assertEquals(2, repo.readLines().size)
        interactor.archive(23, true)
        assertEquals(2, repo.readLines().size)
    }

    @Test
    fun when_a_video_is_registered_and_the_Cast_updated_the_repo_should_be_consistent() {
        fun testAddingCastMember() {
            val video = interactor.register("Video One Register and Archive Test")
            assertEquals(1, repo.readLines().size)
            val coreVideo = video as CoreVideo
            interactor.update(coreVideo.videoId, mutableSetOf(Cast(mutableListOf("Keeley Hawes"))), UpdateType.Add)
            assertEquals(2, repo.readLines().size)
        }

        testAddingCastMember()
    }

    @Test
    fun find_all_should_return_the_correct_size() {
        assertEquals(0, interactor.findAll().size)
        interactor.register("Video 1")
        assertEquals(1, interactor.findAll().size)
        interactor.register("Video 2")
        assertEquals(2, interactor.findAll().size)
    }

    @Test
    fun find_all_with_filter_should_return_the_correct_size() {
        fun setup() {
            interactor.register("Video 1")
            interactor.register("Video 2")
            interactor.register("Video 3")
            interactor.register("Video 4")
            interactor.register("Video 5")
            interactor.register("Video 6")
            interactor.register("Video 7")
            interactor.register("Video 8")
            assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
        }

        setup()
        val data = mutableSetOf<Attribute>(Name("Video 3"))
        val results = interactor.findAll(data)
        assertEquals(1, results.size)
    }

    @Test
    fun exercise_find_all_with_filter_for_code_coverage() {
        registrar.reset()
        val video = interactor.register("Video 1")
        assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
        assertTrue(video is CoreVideo)

        var data = mutableSetOf<Attribute>(Name("Video 1"))
        assertEquals(1, interactor.findAll(data).size)

        val id = (video as? CoreVideo)?.videoId ?: -1
        assertEquals(repo.size() -1, id)

        data = mutableSetOf()
        assertEquals(1, interactor.findAll(data).size)

        data = mutableSetOf(Provider("HBO"))
        assertEquals(0, interactor.findAll(data).size)

        interactor.update(id, data, UpdateType.Add)
        assertEquals(1, interactor.findAll(data).size)

        data = mutableSetOf(Provider("StarTwo"))
        assertEquals(0, interactor.findAll(data).size)
    }

    @Test
    fun exercise_findById_for_code_coverage() {
        val video = interactor.register("Video 1")
        assertTrue(repo.exists() && repo.isFile && repo.size() > 0)
        assertTrue(video is CoreVideo)

        val id = (video as? CoreVideo)?.videoId ?: -1
        assertTrue(interactor.findById(id) is CoreVideo)
        assertEquals(video, interactor.findById(id))
        assertTrue(interactor.findById(23) is VideoError)
    }

    @Test
    fun registering_new_item_bumps_repo_count_by_one() {
        val expected = registrar.videoList.size + 1
        interactor.register("New video")
        assertEquals(expected, registrar.videoList.size)
    }

    @Test
    fun archive_a_registered_video_for_code_coverage() {
        val expected = registrar.videoList.size + 1
        interactor.register("New video")
        assertEquals(expected, registrar.videoList.size)
    }

    @Test
    fun registering_a_video_twice_generates_an_error() {
        val name = "New Name"
        val validVideo = interactor.register(name)
        assertEquals(CoreVideo::class, validVideo::class)
        val invalidVideo = interactor.register(name)
        assertEquals(VideoError::class, invalidVideo::class)
    }

    @Test
    fun find_by_name_correctly_finds_and_does_not_find_a_video() {
        val name = "Video To Find By Name"
        val okVideo = interactor.register(name)
        assertEquals(okVideo, interactor.findByName(name))
        val errorVideo = interactor.findByName("No Such Name")
        assertNotEquals(okVideo, errorVideo)
    }

    @Test
    fun find_by_id_correctly_finds_and_does_not_find_a_video() {
        val name = "Video To Find By Id"
        val okVideo = interactor.register(name)
        if (okVideo is CoreVideo) {
            val id = okVideo.videoId
            assertEquals(okVideo, interactor.findById(id))
        } else fail("Expected a CoreVideo but found a VideoError object!")
        val errorVideo = interactor.findById(-1)
        assertNotEquals(okVideo, errorVideo)
    }

    @Test
    fun update_a_video_in_various_ways_to_exercise_the_update_method() {
        val videoList = listOf(interactor.register("Faux Name"), VideoError(ErrorKey.NoSuchVideo))
        for (video in videoList)
            when (video) {
                is CoreVideo -> {
                    val newName = "The Reel Thing"
                    val castAttribute = Cast(mutableListOf("Star1", "Star2"))
                    val directorsList = mutableListOf("Alfred H.", "Steven S.")
                    val directorsAttribute = Directors(directorsList)
                    val nameAttribute = Name(newName)
                    val providerAttribute = Provider("HBO")
                    val releaseAttribute = Release(0)
                    val seriesAttribute = Series(mutableListOf())
                    val typeAttribute = Type(VideoType.Movie)
                    val attributes = mutableSetOf(
                            castAttribute,
                            directorsAttribute,
                            nameAttribute,
                            providerAttribute,
                            releaseAttribute,
                            seriesAttribute,
                            typeAttribute)
                    interactor.update(video.videoId, attributes, UpdateType.Add)
                    assertEquals(newName, (video.videoData[nameAttribute.attrType] as Name).name)
                    assertEquals(directorsList, (video.videoData[directorsAttribute.attrType] as Directors).directors)
                    interactor.update(video.videoId, mutableSetOf(Cast(mutableListOf("Star2"))), UpdateType.Remove)
                    assertTrue(video.videoData[AttributeType.Cast] != null)
                    interactor.update(video.videoId, mutableSetOf(Provider("HBO")), UpdateType.RemoveAll)
                    assertFalse(video.videoData.contains(AttributeType.Provider))
                }
                is VideoError -> interactor.update(-1, mutableSetOf(), UpdateType.Add)
            }
    }
}
