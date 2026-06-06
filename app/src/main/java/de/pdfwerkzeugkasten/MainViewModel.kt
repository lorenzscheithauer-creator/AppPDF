package de.pdfwerkzeugkasten

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pdfwerkzeugkasten.domain.model.*
import de.pdfwerkzeugkasten.domain.usecase.PageRangeParser
import de.pdfwerkzeugkasten.util.displayName
import de.pdfwerkzeugkasten.util.sizeBytes
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as PdfWerkzeugkastenApp).container
    private val parser = PageRangeParser()
    val userPlan = c.settings.userPlan.stateIn(viewModelScope, SharingStarted.Eagerly, UserPlan.FREE)
    val onboarded = c.settings.onboarded.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val theme = c.settings.theme.stateIn(viewModelScope, SharingStarted.Eagerly, "System")
    val history = c.history.observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val billingState = c.billing.state
    private val _state = MutableStateFlow(AppUiState())
    val state = _state.asStateFlow()
    val tools = listOf(
        PdfTool(ToolType.COMPRESS, "PDF komprimieren", "Dateigröße lokal reduzieren", "Compress", "Top"), PdfTool(ToolType.MERGE, "PDFs zusammenführen", "Mehrere PDFs kombinieren", "Merge", "Top"), PdfTool(ToolType.IMAGES_TO_PDF, "Bilder zu PDF", "Fotos in ein PDF umwandeln", "Image", "Top"), PdfTool(ToolType.SPLIT, "PDF teilen", "Seitenbereiche extrahieren", "Split", "Top"), PdfTool(ToolType.PDF_TO_IMAGES, "PDF zu Bildern", "Seiten als JPG/PNG exportieren", "Export", "Alle", true), PdfTool(ToolType.ROTATE, "Seiten drehen", "Alle Seiten rotieren", "Rotate", "Alle"), PdfTool(ToolType.PROTECT, "Passwort schützen", "PDF verschlüsseln", "Lock", "Alle"), PdfTool(ToolType.UNLOCK, "Passwort entfernen", "Eigene PDF entsperren", "Unlock", "Alle"), PdfTool(ToolType.PREVIEW, "PDF-Vorschau", "Dokument anzeigen", "Preview", "Alle"), PdfTool(ToolType.HISTORY, "Verlauf", "Letzte Vorgänge", "History", "Alle") )
    fun finishOnboarding() = viewModelScope.launch { c.settings.setOnboarded(true) }
    fun selectTool(type: ToolType) { _state.update { AppUiState(currentTool = type, message = null) } }
    fun setUris(uris: List<Uri>) { _state.update { it.copy(inputUris = uris, inputNames = uris.map { u -> getApplication<Application>().displayName(u) }, message = null) } }
    fun setCompression(level: CompressionLevel) { _state.update { it.copy(compression = level) } }
    fun setPageRange(range: String) { _state.update { it.copy(pageRange = range) } }
    fun setPassword(pw: String) { _state.update { it.copy(password = pw) } }
    fun process() = viewModelScope.launch {
        val s = state.value; val app = getApplication<Application>(); val plan = userPlan.value
        _state.update { it.copy(processing = true, message = "PDF wird verarbeitet…", result = null) }
        runCatching {
            val result = when (s.currentTool) {
                ToolType.COMPRESS -> { val u=s.inputUris.first(); val size=app.sizeBytes(u); require(c.limits.canCompress(plan, size)) { "Die Datei ist zu groß für die kostenlose Version." }; c.pdfEngine.compress(u, app.displayName(u), s.compression) }
                ToolType.MERGE -> { require(c.limits.canMerge(plan, s.inputUris.size)) { "Die kostenlose Version kann bis zu 5 PDFs zusammenführen." }; c.pdfEngine.merge(s.inputUris) }
                ToolType.SPLIT -> { val u=s.inputUris.first(); val pages=c.pdfEngine.pageCount(u); val parsed=parser.parse(s.pageRange, pages).getOrThrow(); c.pdfEngine.split(u, app.displayName(u), parsed, s.pageRange.ifBlank { "all" }) }
                ToolType.IMAGES_TO_PDF -> { require(c.limits.canConvertImages(plan, s.inputUris.size)) { "Die kostenlose Version kann bis zu 10 Bilder umwandeln." }; c.pdfEngine.imagesToPdf(s.inputUris, ImageToPdfOptions()) }
                ToolType.ROTATE -> { val u=s.inputUris.first(); c.pdfEngine.rotate(u, app.displayName(u), 90) }
                ToolType.PROTECT -> { val u=s.inputUris.first(); c.pdfEngine.protect(u, app.displayName(u), s.password, allowPrint = true, allowCopy = false) }
                ToolType.UNLOCK -> { val u=s.inputUris.first(); c.pdfEngine.unlock(u, app.displayName(u), s.password) }
                ToolType.PREVIEW, ToolType.PDF_TO_IMAGES, ToolType.HISTORY -> error("Dieses Tool ist im MVP als Vorschau/Pro-Roadmap vorbereitet.")
            }
            c.history.add(HistoryItem(toolType=s.currentTool, displayName=result.fileName, createdAt=System.currentTimeMillis(), outputSizeBytes=result.outputSizeBytes, inputSizeBytes=result.inputSizeBytes, outputUriString=result.uri.toString()))
            result
        }.onSuccess { r -> _state.update { it.copy(processing=false, result=r, message="Fertig") } }.onFailure { e -> _state.update { it.copy(processing=false, message=e.message ?: "Diese PDF ist beschädigt oder kann nicht gelesen werden.") } }
    }
    fun clearHistory() = viewModelScope.launch { c.history.clear() }
    fun restorePurchases() = viewModelScope.launch { c.billing.restore() }
    fun billing() = c.billing
    fun handleIncoming(intent: Intent?) { val action = intent?.action ?: return; val uris = mutableListOf<Uri>(); if (action == Intent.ACTION_SEND) intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(uris::add); if (action == Intent.ACTION_SEND_MULTIPLE) intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(uris::addAll); if (uris.isNotEmpty()) { val type = if (uris.size > 1 && intent.type?.startsWith("image/") == true) ToolType.IMAGES_TO_PDF else if (uris.size > 1) ToolType.MERGE else ToolType.COMPRESS; _state.value = AppUiState(currentTool = type, inputUris = uris, inputNames = uris.map { getApplication<Application>().displayName(it) }, message = "Aus Teilen empfangen") } }
}
data class AppUiState(val currentTool: ToolType = ToolType.COMPRESS, val inputUris: List<Uri> = emptyList(), val inputNames: List<String> = emptyList(), val compression: CompressionLevel = CompressionLevel.MEDIUM, val pageRange: String = "1", val password: String = "", val processing: Boolean = false, val result: PdfOperationResult? = null, val message: String? = null)
