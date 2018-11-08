package com.pajato.argus.core.video

object AttributeFactory {
    private val creatorMap: Map<String, Attribute> = mapOf(
            Cast.key to Cast(),
            Directors.key to Directors(),
            Name.key to Name(),
            Provider.key to Provider(),
            Release.key to Release(),
            Series.key to Series(),
            Type.key to Type()
    )

    fun create(attributeName: String, attributeValue: String): Attribute? =
            creatorMap[attributeName]?.create(attributeValue)
}

enum class AttributeType {
    Cast, Directors, Name, Provider, Release, Series, Type
}

sealed class Attribute {
    abstract val attrType: AttributeType
    abstract val updateByReplace: Boolean
    abstract val values: List<String>

    abstract fun create(arg: String): Attribute
    abstract fun isEqual(to: Attribute): Boolean
    abstract fun updateAdd(attribute: Attribute)
    abstract fun updateRemove(attribute: Attribute)
    abstract fun persist(updateKey: String, videoId: Int, persister: Persister)
}

class Cast(val performers: MutableList<String> = mutableListOf()) : Attribute() {
    companion object {
        const val key = "Cast"
        fun createFrom(arg: String): Cast = Cast(mutableListOf<String>().apply { addAll(listOf(arg)) })
    }

    override val attrType = AttributeType.Cast
    override val updateByReplace = false
    override val values get() = performers

    override fun create(arg: String): Cast = Cast.createFrom(arg)

    override fun isEqual(to: Attribute): Boolean {
        return to is Cast && hasSameEntries(performers, to.performers)
    }

    override fun persist(updateKey: String, videoId: Int, persister: Persister) {
        for (performer in performers)
            persister.update(updateKey, videoId, attrType.name, performer)
    }

    override fun updateAdd(attribute: Attribute) {
        if (attribute !is Cast) return
        performers.addAll(attribute.performers)
    }

    override fun updateRemove(attribute: Attribute) {
        if (attribute !is Cast) return
        performers.removeAll(attribute.performers)
    }
}

class Directors(val directors: MutableList<String> = mutableListOf()) : Attribute() {
    companion object {
        const val key = "Directors"
        fun createFrom(arg: String): Directors = Directors(mutableListOf<String>().apply { addAll(listOf(arg)) })
    }

    override val attrType = AttributeType.Directors
    override val updateByReplace = false
    override val values get() = directors

    override fun create(arg: String): Directors = Directors.createFrom(arg)

    override fun isEqual(to: Attribute): Boolean {
        return to is Directors && hasSameEntries(directors, to.directors)
    }

    override fun persist(updateKey: String, videoId: Int, persister: Persister) {
        for (director in directors)
            persister.update(updateKey, videoId, attrType.name, director)
    }

    override fun updateAdd(attribute: Attribute) {
        if (attribute !is Directors) return
        directors.addAll(attribute.directors)
    }

    override fun updateRemove(attribute: Attribute) {
        if (attribute !is Directors) return
        directors.removeAll(attribute.directors)
    }
}

class Name(val name: String = "") : Attribute() {
    companion object {
        const val key = "Name"
    }

    override val attrType = AttributeType.Name
    override val updateByReplace = true
    override val values get() = listOf(name)

    override fun create(arg: String): Name = Name(arg)

    override fun isEqual(to: Attribute): Boolean {
        return to is Name && name == to.name
    }

    override fun persist(updateKey: String, videoId: Int, persister: Persister) {
        persister.update(updateKey, videoId, attrType.name, name)
    }

    override fun updateAdd(attribute: Attribute) {}
    override fun updateRemove(attribute: Attribute) {}
}

class Provider(val name: String = "") : Attribute() {
    companion object {
        const val key = "Provider"
    }

    override val attrType = AttributeType.Provider
    override val updateByReplace = true
    override val values get() = listOf(name)

    override fun create(arg: String): Provider = Provider(arg)

    override fun isEqual(to: Attribute): Boolean {
        return to is Provider && name == to.name
    }

    override fun persist(updateKey: String, videoId: Int, persister: Persister) {
        persister.update(updateKey, videoId, attrType.name, name)
    }

    override fun updateAdd(attribute: Attribute) {}
    override fun updateRemove(attribute: Attribute) {}
}

class Release(val timeStamp: Long = 0) : Attribute() {
    companion object {
        const val key = "Release"
    }

    override val attrType = AttributeType.Release
    override val updateByReplace = true
    override val values get() = listOf(timeStamp.toString())

    override fun create(arg: String): Release = Release(arg.toLongOrNull() ?: 0L)

    override fun isEqual(to: Attribute): Boolean {
        return to is Release && timeStamp == to.timeStamp
    }

    override fun persist(updateKey: String, videoId: Int, persister: Persister) {
        persister.update(updateKey, videoId, attrType.name, timeStamp.toString())
    }

    override fun updateAdd(attribute: Attribute) {}
    override fun updateRemove(attribute: Attribute) {}
}

class Series(val episodes: MutableList<Episode> = mutableListOf()) : Attribute() {
    companion object {
        const val key = "Series"
        fun createFrom(arg: String): Series = Series(mutableListOf<Episode>().apply {
            val episode = arg.toEpisode()
            if (episode != null) add(episode)
        })
    }

    class Episode(
            val seriesNumber: Int = 0,
            val episodeNumber: Int = 0,
            val episodeData: MutableMap<AttributeType, Attribute> = mutableMapOf()
    )

    override val attrType = AttributeType.Series
    override val updateByReplace = false
    override val values get() = listOf<String>()

    override fun create(arg: String): Series = Series.createFrom(arg)

    override fun isEqual(to: Attribute): Boolean {
        return to == this
    }

    override fun persist(updateKey: String, videoId: Int, persister: Persister) {
        fun persistAttribute(episode: Episode, attributeName: String, attributeValue: String) {
            val episodeValue = "${episode.seriesNumber} ${episode.episodeNumber} $attributeName $attributeValue"
            persister.update(updateKey, videoId, Series.key, episodeValue)
        }

        for (episode in episodes)
            for (episodeAttribute in episode.episodeData.values)
                for (attributeValue in episodeAttribute.values)
                    persistAttribute(episode, episodeAttribute.attrType.name, attributeValue)
    }

    override fun updateAdd(attribute: Attribute) {
        if (attribute !is Series) return
        preventRecursionByFilteringSeriesAttributeFromEachEpisode(attribute)
        remove(attribute.episodes)
        episodes.addAll(attribute.episodes)
    }

    override fun updateRemove(attribute: Attribute) {
        if (attribute !is Series) return
        preventRecursionByFilteringSeriesAttributeFromEachEpisode(attribute)
        remove(attribute.episodes)
    }

    private fun preventRecursionByFilteringSeriesAttributeFromEachEpisode(series: Series) {
        fun pruneSeriesFromEpisodeData(episode: Episode) {
            var attributeToDelete: Attribute? = null
            for (episodeAttribute in episode.episodeData.values)
                if (episodeAttribute is Series)
                    attributeToDelete = episodeAttribute
            if (attributeToDelete != null)
                episode.episodeData.remove(attributeToDelete.attrType)
        }

        for (episode in series.episodes) {
            pruneSeriesFromEpisodeData(episode)
        }

    }

    private fun remove(candidateEpisodes: List<Episode>) {
        fun getMatchingEpisodes(candidateEpisodes: List<Episode>): List<Episode> {
            val list = mutableListOf<Episode>()
            candidateEpisodes.forEach {
                for (episode in episodes)
                    if (it.seriesNumber == episode.seriesNumber && it.episodeNumber == episode.episodeNumber)
                        list.add(episode)
            }
            return list
        }

        val episodesToDelete = getMatchingEpisodes(candidateEpisodes)
        episodes.removeAll(episodesToDelete)
    }
}

class Type(val type: VideoType = VideoType.Movie) : Attribute() {
    companion object {
        const val key = "Type"
    }

    override val attrType = AttributeType.Type
    override val updateByReplace = true
    override val values get() = listOf(type.name)

    override fun create(arg: String): Type = Type(
            try {
                VideoType.valueOf(arg)
            } catch (exc: Exception) {
                VideoType.Error
            }
    )

    override fun isEqual(to: Attribute): Boolean {
        return to is Type && type == to.type
    }

    override fun persist(updateKey: String, videoId: Int, persister: Persister) {
        persister.update(updateKey, videoId, attrType.name, type.name)
    }

    override fun updateAdd(attribute: Attribute) {}
    override fun updateRemove(attribute: Attribute) {}
}

fun hasSameEntries(first: List<String>, second: List<String>): Boolean {
    if (first.size != second.size) return false
    for (string in first)
        if (!second.contains(string)) return false
    return true
}

private fun String.toEpisode(): Series.Episode? {
    val episodeRegex = """^(\d+) (\d+) ([\w]+) (.+)$""".toRegex()
    fun getEpisodeData(match: MatchResult): MutableMap<AttributeType, Attribute>? {
        val attributeName = match.groupValues[3]
        val attributeArg = match.groupValues[4]
        val attribute = AttributeFactory.create(attributeName, attributeArg) ?: return null
        return mutableMapOf(AttributeType.valueOf(attributeName) to attribute)
    }
    return episodeRegex.find(this)?.let {
        val seriesNumber = it.groupValues[1].toInt()
        val episodeNumber = it.groupValues[2].toInt()
        val episodeData = getEpisodeData(it) ?: return null
        Series.Episode(seriesNumber, episodeNumber, episodeData)
    }
}
