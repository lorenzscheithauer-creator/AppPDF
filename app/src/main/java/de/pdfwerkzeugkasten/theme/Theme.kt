package de.pdfwerkzeugkasten.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable fun PdfWerkzeugkastenTheme(theme: String = "System", content: @Composable () -> Unit) { val dark = when (theme) { "Dunkel" -> true; "Hell" -> false; else -> isSystemInDarkTheme() }; MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFF8AB4F8), secondary = Color(0xFFFFB4AB)) else lightColorScheme(primary = Color(0xFF0D47A1), secondary = Color(0xFFC62828), background = Color(0xFFF7F9FC)), typography = Typography(), content = content) }
