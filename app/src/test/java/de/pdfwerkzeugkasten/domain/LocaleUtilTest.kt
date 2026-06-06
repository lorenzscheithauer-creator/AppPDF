package de.pdfwerkzeugkasten.domain

import de.pdfwerkzeugkasten.util.LocaleUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleUtilTest {
    @Test fun supportedLanguageTagsAreStable() { assertEquals("de", LocaleUtil.GERMAN); assertEquals("en", LocaleUtil.ENGLISH); assertEquals("system", LocaleUtil.SYSTEM) }
}
