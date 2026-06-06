package de.pdfwerkzeugkasten.domain

import de.pdfwerkzeugkasten.domain.usecase.PageRangeParser
import org.junit.Assert.*
import org.junit.Test

class PageRangeParserTest {
    private val parser = PageRangeParser()
    @Test fun parsesRangesAndSingles() { assertEquals(listOf(1,2,3,5,8,9,10), parser.parse("1-3, 5, 8-10", 10).getOrThrow()) }
    @Test fun rejectsOutOfBounds() { assertTrue(parser.parse("1-99", 10).isFailure) }
    @Test fun blankMeansAllPages() { assertEquals((1..3).toList(), parser.parse("", 3).getOrThrow()) }
}
