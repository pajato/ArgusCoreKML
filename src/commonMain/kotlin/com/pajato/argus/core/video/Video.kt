package com.pajato.argus.core.video

enum class VideoType {
    Error, Movie, MovieSeries, TvShow, TvSeries
}

enum class ErrorKey {
    AlreadyExists, NoSuchVideo, Ok, UnsupportedVideoType
}

sealed class Video

class CoreVideo(val videoId: Int, val videoData: MutableMap<AttributeType, Attribute> = mutableMapOf()) : Video() {
    var archived = false

    fun update(videoData: MutableSet<Attribute> = mutableSetOf(), key: String, persister: Persister? = null): Video {
        val updater = UpdaterFactory.create(key, this) ?: return this
        for (attribute in videoData) {
            updater.update(attribute)
            if (persister != null)
                attribute.persist(key, videoId, persister)
        }
        return this
    }
}

class VideoError(val key: ErrorKey, val message: String = "") : Video()
