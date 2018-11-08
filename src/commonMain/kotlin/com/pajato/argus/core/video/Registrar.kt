package com.pajato.argus.core.video

class Registrar(eventStore: EventStore) : VideoRegistrar {
    val videoList : MutableList<Video> = mutableListOf()
    private val persister = Persister(eventStore)
    private val idMap: MutableMap<kotlin.String, Int> = mutableMapOf()
    private val videoMap: MutableMap<Int, CoreVideo> = mutableMapOf()
    private var nextUniqueId = 0

    init {
        nextUniqueId = persister.load().size
    }

    fun reset() {
        videoList.clear()
        idMap.clear()
        videoMap.clear()
        persister.clear()
        nextUniqueId = 0
    }

    override fun archive(videoId: Int, state: Boolean): Video {
        fun archive(video: CoreVideo) {
            video.archived = state
            persister.archive(video)
        }

        val video = findById(videoId)
        if (video is CoreVideo)
            archive(video)
        return video
    }

    override fun findAll(filterData: MutableSet<Attribute>): List<CoreVideo> {
        fun matches(video: CoreVideo): Boolean {
            filterData.forEach {
                val attribute = video.videoData[it.attrType] ?: return false
                if (!attribute.isEqual(it)) return false
            }
            return true
        }

        val list = videoMap.values.toList()
        return if (filterData.size == 0) list else list.filter { matches(it) }
    }

    override fun findById(videoId: Int): Video {
        return videoMap[videoId] ?: VideoError(ErrorKey.NoSuchVideo)
    }

    override fun findByName(name: kotlin.String): Video {
        val id = idMap.getOrElse(name) { -1 }
        return if (id != -1) findById(id) else VideoError(ErrorKey.NoSuchVideo)
    }

    override fun register(name: kotlin.String): Video {
        fun createVideo(): Video {
            fun processAttributes(video: CoreVideo) {
                fun registerVideoWithAttributes(video: CoreVideo, attrs: MutableMap<AttributeType, Attribute>) {
                    videoList.add(video)
                    val id = video.videoId
                    idMap[name] = id
                    videoMap[id] = video
                    video.videoData.putAll(attrs)
                }

                val attributes : MutableMap<AttributeType, Attribute> = mutableMapOf()
                val nameAttribute = Name(name)
                attributes[nameAttribute.attrType] = nameAttribute
                registerVideoWithAttributes(video, attributes)
            }

            val video = CoreVideo(nextUniqueId++)
            processAttributes(video)
            persister.register(video, name)
            return video
        }

        return if (idMap[name] != null) VideoError(ErrorKey.AlreadyExists) else createVideo()
    }

    override fun update(videoId: Int, videoData: MutableSet<Attribute>, updateType: UpdateType): Video {
        val video = findById(videoId)
        if (video is CoreVideo)
            video.update(videoData, updateType.name, persister)
        return video
    }
}
