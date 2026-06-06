package de.pdfwerkzeugkasten.domain.model

data class SearchableToolText(
    val id: ToolType,
    val title: String,
    val description: String,
    val keywords: String
)

object ToolSearchFilter {
    fun filter(query: String, tools: List<SearchableToolText>): List<SearchableToolText> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return tools
        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        return tools.filter { tool ->
            val haystack = listOf(tool.title, tool.description, tool.keywords, tool.id.name)
                .joinToString(" ")
                .lowercase()
            tokens.all { haystack.contains(it) }
        }
    }
}
