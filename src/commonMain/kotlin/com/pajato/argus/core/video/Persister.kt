package com.pajato.argus.core.video

interface EventStore {
    val path: String
    val isFile: Boolean
    fun appendText(line: String)
    fun clear()
    fun exists(): Boolean
    fun forEachLine(action: (line: String) -> Unit)
    fun size(): Int
    fun readLines(): List<String>
}

expect fun createEventStore(path: String): EventStore

class Persister(val eventStore: EventStore) {
    companion object {
        val baseRegex = """^([\w]+) (\d+) (.*$)""".toRegex()
    }

    fun archive(video: CoreVideo) {
        ArchiveEvent(video.videoId.toString()).store(this)
    }

    fun clear() = eventStore.clear()

    fun load(): MutableMap<Int, CoreVideo> {
        val idMap = mutableMapOf<Int, CoreVideo>()
        fun parseAndExecuteEvent(line: String) {
            fun toEvent(eventName: String, videoId: String, rest: String): VideoEvent? =
                    videoEventMap[eventName]?.create(videoId, rest)

            baseRegex.find(line)?.apply {
                val eventName = groupValues[1]
                val videoId = groupValues[2]
                val rest = groupValues[3]
                val event = toEvent(eventName, videoId, rest) ?: return
                event.load(idMap)
            }
        }

        eventStore.forEachLine { line ->
            parseAndExecuteEvent(line)
        }
        return idMap
    }

    fun persist(text: String) {
        eventStore.appendText(text)
    }

    fun register(video: CoreVideo, name: String) {
        RegisterEvent(video.videoId.toString(), name).store(this)
    }

    fun update(updateType: String, videoId: Int, attributeName: String, attributeValue: String) {
        UpdateEvent(videoId.toString(), attributeName, attributeValue, updateType).store(this)
    }

}

private val videoEventMap = mapOf(
        ArchiveEvent.eventName to ArchiveEvent(),
        RegisterEvent.eventName to RegisterEvent(),
        UpdateEvent.eventName to UpdateEvent()
)

