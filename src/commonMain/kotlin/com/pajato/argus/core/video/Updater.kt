package com.pajato.argus.core.video

enum class UpdateType {
    Add, Remove, RemoveAll
}

object UpdaterFactory {
    private val updaterMap = mapOf(
            AddUpdater.updaterName to AddUpdater(),
            RemoveUpdater.updaterName to RemoveUpdater(),
            RemoveAllUpdater.updaterName to RemoveAllUpdater()
    )

    fun create(key: String, video: CoreVideo): Updater? = updaterMap[key]?.create(video)
}

sealed class Updater {
    abstract fun create(video: CoreVideo): Updater?
    abstract fun update(attribute: Attribute)
}

class AddUpdater(private val video: CoreVideo = CoreVideo(0)) : Updater() {
    companion object {
        const val updaterName = "Add"
    }

    override fun create(video: CoreVideo): Updater? = AddUpdater(video)

    override fun update(attribute: Attribute) {
        val videoAttribute = video.videoData[attribute.attrType]
        if (videoAttribute == null || attribute.updateByReplace)
            video.videoData[attribute.attrType] = attribute
        else
            videoAttribute.updateAdd(attribute)
    }
}

class RemoveUpdater(private val video: CoreVideo = CoreVideo(0)) : Updater() {
    companion object {
        const val updaterName = "Remove"
    }

    override fun create(video: CoreVideo): Updater? = RemoveUpdater(video)

    override fun update(attribute: Attribute) {
        val videoAttribute = video.videoData[attribute.attrType] ?: return
        if (videoAttribute.updateByReplace)
            video.videoData.remove(attribute.attrType)
        else
            videoAttribute.updateRemove(attribute)
    }
}

class RemoveAllUpdater(private val video: CoreVideo = CoreVideo(0)) : Updater() {
    companion object {
        const val updaterName = "RemoveAll"
    }

    override fun create(video: CoreVideo): Updater? = RemoveAllUpdater(video)

    override fun update(attribute: Attribute) {
        if (!video.videoData.containsKey(attribute.attrType)) return
        video.videoData.remove(attribute.attrType)
    }
}
