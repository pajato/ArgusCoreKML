package com.pajato.argus.core.video

actual fun createEventStore(path: String): EventStore {
    return object : EventStore {
        private val eventStore = mutableListOf<String>()
        override val isFile = true
        override val path: String = path

        override fun appendText(line: String) {
            eventStore.addAll(line.substring(0, line.length - 1).split("\n"))
        }

        override fun clear() {
            eventStore.clear()
        }

        override fun exists() = true

        override fun size() = readLines().size

        override fun forEachLine(action: (line: String) -> Unit) {
            for (line in readLines()) {
                action(line)
            }
        }

        override fun readLines(): List<String> = eventStore
    }
}
