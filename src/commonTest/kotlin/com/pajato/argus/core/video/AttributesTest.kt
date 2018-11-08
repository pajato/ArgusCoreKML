package com.pajato.argus.core.video

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttributesTest {
    private val repo = createEventStore("build/tmp/argus/attributes-test")
    private val persister = Persister(repo)

    @Test
    fun testSomeCastCode() {
        val nameValue = "fred"
        val cast = Cast(mutableListOf())
        assertFalse(cast.updateByReplace)
        cast.isEqual(AttributeFactory.create(AttributeType.Name.name, nameValue)!!)
        cast.isEqual(Cast.createFrom(nameValue))
        assertEquals(0, cast.performers.size)
        assertEquals(0, cast.values.size)
        cast.updateAdd(Cast.createFrom(nameValue))
        assertEquals(1, cast.performers.size)
        assertEquals(1, cast.values.size)
        cast.updateRemove(Cast.createFrom(nameValue))
        assertEquals(0, cast.performers.size)
        assertEquals(0, cast.values.size)
        cast.updateAdd(AttributeFactory.create(AttributeType.Name.name, nameValue)!!)
        cast.updateRemove(AttributeFactory.create(AttributeType.Name.name, nameValue)!!)
    }

    @Test
    fun testSomeDirectorsCode() {
        val nameValue = "fred"
        val directors = Directors(mutableListOf())
        assertFalse(directors.updateByReplace)
        directors.isEqual(AttributeFactory.create(AttributeType.Name.name, nameValue)!!)
        directors.isEqual(Directors.createFrom(nameValue))
        directors.updateAdd(Directors.createFrom(nameValue))
        assertEquals(1, directors.directors.size)
        assertEquals(directors.directors, directors.values)
        directors.updateRemove(Directors.createFrom(nameValue))
        assertEquals(0, directors.directors.size)
        assertEquals(directors.directors, directors.values)
        directors.updateAdd(AttributeFactory.create(AttributeType.Name.name, nameValue)!!)
        directors.updateRemove(AttributeFactory.create(AttributeType.Name.name, nameValue)!!)
    }

    @Test
    fun testSomeNameCode() {
        val nameValue = "fred"
        val name = Name(nameValue)
        assertTrue(name.updateByReplace)
        name.updateAdd(name)
        name.updateRemove(name)
        assertEquals(true, name.isEqual(AttributeFactory.create(AttributeType.Name.name, nameValue)!!))
        assertEquals(false, name.isEqual(AttributeFactory.create(AttributeType.Provider.name, nameValue)!!))
        assertEquals(1, name.values.size)
        assertEquals(nameValue, name.values[0])
    }

    @Test
    fun testSomeProviderCode() {
        val nameValue = "fred"
        val provider = Provider(nameValue)
        assertTrue(provider.updateByReplace)
        provider.updateAdd(provider)
        provider.updateRemove(provider)
        provider.isEqual(AttributeFactory.create(AttributeType.Name.name, "fred")!!)
        assertEquals(true, provider.isEqual(AttributeFactory.create(AttributeType.Provider.name, nameValue)!!))
        assertEquals(false, provider.isEqual(AttributeFactory.create(AttributeType.Name.name, nameValue)!!))
        assertEquals(1, provider.values.size)
        assertEquals(nameValue, provider.values[0])
    }

    @Test
    fun testValidAndInvalidReleaseAttributes() {
        val timeStamp = 0L
        val validRelease = Release(timeStamp)
        val attrName = AttributeType.Release.name
        assertTrue(validRelease.updateByReplace)
        validRelease.updateAdd(validRelease)
        validRelease.updateRemove(validRelease)
        assertTrue(validRelease.isEqual(AttributeFactory.create(attrName, timeStamp.toString())!!))
        assertFalse(validRelease.isEqual(AttributeFactory.create(Name.key, timeStamp.toString())!!))
        assertEquals(1, validRelease.values.size)
        assertEquals(timeStamp.toString(), validRelease.values[0])
        validRelease.isEqual(AttributeFactory.create(AttributeType.Name.name, "Fred")!!)
        val invalidRelease = Release().create("invalid-non-numeric-timestamp")
        assertEquals(0L, invalidRelease.timeStamp)
    }

    @Test
    fun testTheSeriesAttribute() {
        val name = "The White Walker"
        val episodeData = mutableMapOf<AttributeType, Attribute>(AttributeType.Name to Name(name))
        val series = Series(mutableListOf(Series.Episode(1, 1, episodeData)))
        fun testBasicSeries() {
            assertEquals(1, series.episodes.size)
            assertEquals(1, series.episodes[0].seriesNumber)
            assertEquals(1, series.episodes[0].episodeNumber)
            assertEquals(name, (series.episodes[0].episodeData[AttributeType.Name] as Name).name)
            assertEquals(AttributeType.Series, series.attrType)
            assertEquals(false, series.updateByReplace)
            assertEquals(true, series.isEqual(series))
            assertEquals(0, series.values.size)
        }

        fun testSeriesUpdateAdd() {
            series.updateAdd(Name(name))
            series.updateAdd(Series(mutableListOf(Series.Episode(1, 2, episodeData))))
            assertEquals(2, series.episodes.size)
            series.persist(UpdateType.Add.name, 0, persister)
            assertEquals(2, persister.eventStore.readLines().size)
        }

        fun testSeriesUpdateRemove() {
            series.updateRemove(Series(mutableListOf(Series.Episode(1, 2, episodeData))))
            assertEquals(1, series.episodes.size)
            series.updateRemove(Name("fred"))
            series.updateRemove(Series(mutableListOf(Series.Episode(6, 6, episodeData))))
        }

        fun testSeriesFiltering() {
            val testData = mutableMapOf<AttributeType, Attribute>(AttributeType.Series to Series(mutableListOf()))
            series.updateAdd(Series(mutableListOf(Series.Episode(1, 3, testData))))
            assertEquals(2, series.episodes.size)
        }

        repo.clear()
        testBasicSeries()
        testSeriesUpdateAdd()
        testSeriesUpdateRemove()
        testSeriesFiltering()
    }

    @Test
    fun testSomeTypeCode() {
        val movieType = VideoType.Movie
        val type = Type(movieType)
        assertTrue(type.updateByReplace)
        type.updateAdd(type)
        type.updateRemove(type)
        type.isEqual(AttributeFactory.create(AttributeType.Name.name, "fred")!!)
        assertEquals(1, type.values.size)
        assertEquals(movieType.name, type.values[0])
    }

    @Test
    fun testTheCastCreator() {
        val name = "some star"
        val cast = Cast.createFrom(name)
        assertTrue(cast.performers.contains(name))
    }

    @Test
    fun testTheDirectorsCreator() {
        val name = "the director"
        val directors = Directors.createFrom(name)
        assertTrue(directors.directors.contains(name))
    }

    @Test
    fun testTheNameCreator() {
        val videoName = "the video"
        val name = AttributeFactory.create(AttributeType.Name.name, videoName)
        assertTrue(name is Name && name.name == videoName)
    }

    @Test
    fun testTheProviderCreator() {
        val videoProvider = "Netflix"
        val provider = AttributeFactory.create(AttributeType.Provider.name, videoProvider)
        assertTrue(provider is Provider && provider.name == videoProvider)
    }

    @Test
    fun testTheReleaseCreator() {
        val videoRelease = "1456223331"
        val release = AttributeFactory.create(AttributeType.Release.name, videoRelease)
        assertTrue(release is Release && release.timeStamp == videoRelease.toLong())
    }

    @Test
    fun testTheSeriesCreator() {
        var episodeAsString = "1 1 "
        var series = Series().create(episodeAsString)
        assertEquals(0, series.episodes.size)
        AttributeFactory.create(AttributeType.Type.name, "Error")
        episodeAsString = "1 1 InvalidAttributeName xxx"
        series = Series().create(episodeAsString)
        assertEquals(0, series.episodes.size)
    }

    @Test
    fun testTheTypeCreator() {
        val name = "Movie"
        val validType = AttributeFactory.create(AttributeType.Type.name, name)
        assertTrue(validType is Type && validType.type.name == name)
        val invalidType = AttributeFactory.create(AttributeType.Type.name, "InvalidType")
        assertTrue(invalidType is Type && invalidType.type.name == "Error")
    }

    @Test
    fun testAnInvalidAttributeCreator() {
        val name = "InvalidAttributeName"
        val invalidAttribute = AttributeFactory.create(name, "")
        assertEquals(null, invalidAttribute)
    }

    @Test
    fun whenArgIsNotAValidEpisodeRegex_testThatToEpisodeReturnsNoEpisode() {
        val episodeAsString = "1 1 "
        val series = Series.createFrom(episodeAsString)
        assertEquals(0, series.episodes.size)
    }

    @Test
    fun whenArgIsAValidEpisodeRegex_testThatToEpisodeReturnsAValidEpisode() {
        val name = "Ned's Demise"
        val episodeAsString = "1 7 Name $name"
        val series = Series.createFrom(episodeAsString)
        assertEquals(1, series.episodes.size)
        assertEquals(1, series.episodes[0].seriesNumber)
        assertEquals(7, series.episodes[0].episodeNumber)
        val actualName = (series.episodes[0].episodeData[AttributeType.Name] as Name).name
        assertEquals(name, actualName)
    }
}
