package com.pajato.argus.core.video

import kotlin.test.Test
import kotlin.test.assertEquals

class UpdaterTest {
    private val repo = createEventStore("build/tmp/argus/updater-test")
    private val uut = Persister(repo)

    @Test
    fun exercise_the_updater_factory_via_CoreVideo_methods_for_code_coverage() {
        val video = CoreVideo(0)
        assertEquals(video, video.update(key = "fred", persister = uut))
        assertEquals(video, video.update(key = "fred"))
    }
}
