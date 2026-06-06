package de.pdfwerkzeugkasten.domain.usecase

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileNameGenerator(private val now: () -> Date = { Date() }) {
    private val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    fun compressed(original: String) = "compressed_${sanitize(base(original))}.pdf"
    fun merged() = "merged_${day.format(now())}.pdf"
    fun imagesToPdf() = "images_to_pdf_${day.format(now())}.pdf"
    fun split(original: String, range: String) = "split_${sanitize(base(original))}_pages_${sanitize(range).ifBlank { "all" }}.pdf"
    fun rotated(original: String) = "rotated_${sanitize(base(original))}.pdf"
    fun protected(original: String) = "protected_${sanitize(base(original))}.pdf"
    fun unlocked(original: String) = "unlocked_${sanitize(base(original))}.pdf"
    private fun base(name: String) = name.substringAfterLast('/').removeSuffix(".pdf")
    private fun sanitize(value: String) = value.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').take(80)
}
