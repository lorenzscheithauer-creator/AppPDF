package de.pdfwerkzeugkasten.domain

import de.pdfwerkzeugkasten.domain.model.CompressionLevel
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressionLevelTest { @Test fun strongUsesLowestQuality() { assertTrue(CompressionLevel.STRONG.imageQuality < CompressionLevel.MEDIUM.imageQuality); assertTrue(CompressionLevel.MEDIUM.imageQuality < CompressionLevel.LIGHT.imageQuality) } }
