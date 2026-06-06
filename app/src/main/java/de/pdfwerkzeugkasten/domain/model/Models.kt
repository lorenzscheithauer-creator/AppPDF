package de.pdfwerkzeugkasten.domain.model

import android.net.Uri

enum class ToolType { COMPRESS, MERGE, SPLIT, IMAGES_TO_PDF, PDF_TO_IMAGES, ROTATE, PROTECT, UNLOCK, PREVIEW, HISTORY }
enum class CompressionLevel(val label: String, val imageQuality: Int) { LIGHT("Leicht", 92), MEDIUM("Mittel", 75), STRONG("Stark", 55) }
enum class UserPlan { FREE, PRO }
enum class JobStatus { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }

data class PdfTool(val id: ToolType, val title: String, val description: String, val icon: String, val category: String, val isComingSoon: Boolean = false)
data class PdfJob(val id: String, val type: ToolType, val inputUris: List<Uri>, val outputUri: Uri? = null, val status: JobStatus = JobStatus.QUEUED, val progress: Int = 0, val createdAt: Long = System.currentTimeMillis(), val completedAt: Long? = null, val errorMessage: String? = null)
data class HistoryItem(val id: Long = 0, val toolType: ToolType, val displayName: String, val createdAt: Long, val outputSizeBytes: Long, val inputSizeBytes: Long, val outputUriString: String? = null)
data class PdfOperationResult(val fileName: String, val uri: Uri, val inputSizeBytes: Long, val outputSizeBytes: Long, val mimeType: String = "application/pdf") { val savingsPercent: Int get() = if (inputSizeBytes <= 0) 0 else ((inputSizeBytes - outputSizeBytes).coerceAtLeast(0) * 100 / inputSizeBytes).toInt() }
data class ImageToPdfOptions(val paperFormat: PaperFormat = PaperFormat.A4, val orientation: PageOrientation = PageOrientation.AUTO, val margin: PageMargin = PageMargin.SMALL, val quality: ImageQuality = ImageQuality.MEDIUM)
enum class PaperFormat { A4, LETTER, ORIGINAL }
enum class PageOrientation { AUTO, PORTRAIT, LANDSCAPE }
enum class PageMargin(val points: Float) { NONE(0f), SMALL(24f), MEDIUM(48f) }
enum class ImageQuality(val jpegQuality: Int) { HIGH(92), MEDIUM(78), LOW(60) }
