package de.pdfwerkzeugkasten.domain.usecase

class PageRangeParser {
    fun parse(input: String, pageCount: Int): Result<List<Int>> = runCatching {
        require(pageCount > 0) { "PDF ohne Seiten." }
        if (input.isBlank()) return@runCatching (1..pageCount).toList()
        input.split(',').flatMap { part ->
            val token = part.trim(); require(token.isNotEmpty()) { "Leerer Seitenbereich." }
            if ('-' in token) {
                val bounds = token.split('-'); require(bounds.size == 2) { "Ungültiger Seitenbereich: $token" }
                val start = bounds[0].trim().toInt(); val end = bounds[1].trim().toInt()
                require(start in 1..pageCount && end in 1..pageCount && start <= end) { "Seitenbereich außerhalb des Dokuments: $token" }
                (start..end).toList()
            } else {
                val page = token.toInt(); require(page in 1..pageCount) { "Seite außerhalb des Dokuments: $page" }; listOf(page)
            }
        }.distinct()
    }
}
