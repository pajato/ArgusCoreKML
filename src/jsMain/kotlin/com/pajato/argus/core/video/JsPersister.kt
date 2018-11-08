package com.pajato.argus.core.video

actual fun createEventStore(path: String) : EventStore {
    return object : EventStore {
        private val eventStore = mutableListOf<String>()
        override val isFile = true
        override val path: String = path

        override fun appendText(line: String) {
            eventStore.addAll(line.trim().split("\n"))
        }

        override fun clear() {
            eventStore.clear()
        }

        override fun exists() = true

        override fun size() = eventStore.size

        override fun forEachLine(action: (line: String) -> Unit) {
            for (line in eventStore) {
                action(line)
            }
        }

        override fun readLines() = eventStore
    }
}
