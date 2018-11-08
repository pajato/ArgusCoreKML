package com.pajato.argus.core.video

sealed class VideoEvent {
    abstract val key: String
    abstract fun create(videoId: String, rest: String): VideoEvent?
    abstract fun load(videoIdMap: MutableMap<Int, CoreVideo>)
    abstract fun store(persister: Persister)
}

class ArchiveEvent(val videoId: String = "", val state: String = "") : VideoEvent() {
    companion object {
        const val eventName = "Archive"
    }

    override val key = eventName
    override fun create(videoId: String, rest: String): ArchiveEvent? = ArchiveEvent(videoId, rest)
    override fun load(videoIdMap: MutableMap<Int, CoreVideo>) {
        val videoIdAsInt = videoId.toIntOrNull() ?: return
        val video = videoIdMap[videoIdAsInt] ?: return
        video.archived = state.toBoolean()
    }

    override fun store(persister: Persister) {
        persister.persist("$key $videoId $state\n")
    }
}

class RegisterEvent(val videoId: String = "", val videoName: String = "") : VideoEvent() {
    companion object {
        const val eventName = "Register"
    }

    override val key = eventName
    override fun create(videoId: String, rest: String): RegisterEvent? = RegisterEvent(videoId, rest)
    override fun load(videoIdMap: MutableMap<Int, CoreVideo>) {
        val videoIdAsInt = videoId.toIntOrNull() ?: return
        val data = mutableMapOf<AttributeType, Attribute>()
        data[AttributeType.Name] = Name(videoName)
        val video = CoreVideo(videoIdAsInt, data)
        videoIdMap[videoIdAsInt] = video
    }

    override fun store(persister: Persister) {
        persister.persist("$key $videoId $videoName\n")
    }
}

class UpdateEvent(
        val videoId: String = "",
        val attributeName: String = "",
        val attributeValue: String = "",
        val updateType: String = ""
) : VideoEvent() {
    companion object {
        const val eventName = "Update"
    }

    override val key = eventName
    override fun create(videoId: String, rest: String): UpdateEvent? {
        val updateRegex = """^([\w]+) ([\w]+) (.*$)""".toRegex()
        return updateRegex.find(rest)?.let {
            val updateType = it.groupValues[1]
            val attributeName = it.groupValues[2]
            val attributeValue = it.groupValues[3]
            UpdateEvent(videoId, attributeName, attributeValue, updateType)
        }
    }

    override fun load(videoIdMap: MutableMap<Int, CoreVideo>) {
        val videoIdAsInt = videoId.toIntOrNull() ?: return
        val video = videoIdMap[videoIdAsInt] ?: return
        val attribute = AttributeFactory.create(attributeName, attributeValue) ?: return
        video.update(mutableSetOf(attribute), updateType)
    }

    override fun store(persister: Persister) {
        persister.persist("$key $videoId $updateType $attributeName $attributeValue\n")
    }
}
