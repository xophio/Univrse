package com.universe.remote.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {
    @Test fun parsesCentisecondsAndSortsLines() {
        val lines = LrcParser.parse("[00:12.50]Second\n[00:01.2]First")
        assertEquals(listOf(1_200L, 12_500L), lines.map { it.timeMs })
        assertEquals(listOf("First", "Second"), lines.map { it.text })
    }

    @Test fun expandsMultipleTimestampsAndAppliesOffset() {
        val lines = LrcParser.parse("[offset:+250]\n[00:03.000][00:09.00]Again")
        assertEquals(listOf(3_250L, 9_250L), lines.map { it.timeMs })
        assertEquals(listOf("Again", "Again"), lines.map { it.text })
    }

    @Test fun ignoresMetadataBlankAndMalformedRows() {
        val lines = LrcParser.parse("[ar:Artist]\n[00:01.00]\nhello\n[01:02.003]Kept <01:02.100>")
        assertEquals(1, lines.size)
        assertEquals(62_003L, lines.single().timeMs)
        assertEquals("Kept", lines.single().text)
    }
}
