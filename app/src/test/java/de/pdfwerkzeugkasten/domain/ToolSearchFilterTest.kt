package de.pdfwerkzeugkasten.domain

import de.pdfwerkzeugkasten.domain.model.SearchableToolText
import de.pdfwerkzeugkasten.domain.model.ToolSearchFilter
import de.pdfwerkzeugkasten.domain.model.ToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSearchFilterTest {
    private val tools = listOf(
        SearchableToolText(ToolType.MERGE, "PDFs zusammenführen", "Mehrere PDFs kombinieren", "merge combine join"),
        SearchableToolText(ToolType.PROTECT, "Passwort schützen", "PDF verschlüsseln", "password secure lock"),
        SearchableToolText(ToolType.IMAGES_TO_PDF, "Bilder zu PDF", "Fotos in ein PDF umwandeln", "images photos jpg")
    )

    @Test fun blankQueryReturnsAllTools() { assertEquals(tools, ToolSearchFilter.filter(" ", tools)) }
    @Test fun filtersTitleDescriptionAndKeywords() { assertEquals(listOf(ToolType.MERGE), ToolSearchFilter.filter("combine", tools).map { it.id }); assertEquals(listOf(ToolType.PROTECT), ToolSearchFilter.filter("Passwort", tools).map { it.id }) }
    @Test fun allTokensMustMatch() { assertEquals(listOf(ToolType.IMAGES_TO_PDF), ToolSearchFilter.filter("bilder jpg", tools).map { it.id }) }
    @Test fun noMatchReturnsEmptyList() { assertTrue(ToolSearchFilter.filter("ocr", tools).isEmpty()) }
}
