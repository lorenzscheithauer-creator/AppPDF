package de.pdfwerkzeugkasten.domain

import de.pdfwerkzeugkasten.util.LocaleUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleUtilTest {
    @Test fun supportedLanguageTagsIncludeStoreLocales() {
        assertEquals("system", LocaleUtil.SYSTEM)
        assertEquals(listOf("de", "en", "es", "fr", "it", "pt", "tr"), LocaleUtil.SUPPORTED_LANGUAGES.map { it.tag })
    }

    @Test fun languageTagsAreUnique() {
        val tags = LocaleUtil.SUPPORTED_LANGUAGES.map { it.tag }
        assertTrue(tags.size == tags.toSet().size)
    }
}
