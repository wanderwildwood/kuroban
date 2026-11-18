package com.wanderwildwood.kuroban.engine

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bundled GNU Go is a built binary, so it is tied to a record: engine/libgnugo.so.sha256,
 * written by engine/build-gnugo.sh. A libgnugo.so that does not match it was not made by that
 * script from the tarball beside it, or was made without its record being committed.
 */
class EngineHashTest {
    @Test fun theBundledEngineIsTheOneOnRecord() {
        val engine = File("src/main/jniLibs/arm64-v8a/libgnugo.so")
        val record = File("../engine/libgnugo.so.sha256").readText().trim()
        val expected = record.substringBefore(' ')
        assertEquals("libgnugo.so", record.substringAfterLast(' '))
        val actual = MessageDigest.getInstance("SHA-256").digest(engine.readBytes())
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, actual)
    }
}
