package de.pdfwerkzeugkasten.domain

import de.pdfwerkzeugkasten.domain.usecase.FileNameGenerator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class FileNameGeneratorTest {
    private val names = FileNameGenerator { Date(0) }
    @Test fun createsSafeNames() { assertEquals("compressed_my_file.pdf", names.compressed("my file.pdf")); assertEquals("merged_1970-01-01.pdf", names.merged()); assertEquals("split_doc_pages_1-3_5.pdf", names.split("doc.pdf", "1-3, 5")) }
}
