package de.pdfwerkzeugkasten

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pdfwerkzeugkasten.domain.model.*
import de.pdfwerkzeugkasten.domain.usecase.PageRangeParser
import de.pdfwerkzeugkasten.util.LocaleUtil
import de.pdfwerkzeugkasten.util.displayName
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as PdfWerkzeugkastenApp).container
    private val parser = PageRangeParser()
    val userPlan = c.settings.userPlan.stateIn(viewModelScope, SharingStarted.Eagerly, UserPlan.FREE)
    val onboarded = c.settings.onboarded.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val theme = c.settings.theme.stateIn(viewModelScope, SharingStarted.Eagerly, "System")
    val language = c.settings.language.stateIn(viewModelScope, SharingStarted.Eagerly, LocaleUtil.SYSTEM)
    val history = c.history.observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val billingState = c.billing.state
    private val _state = MutableStateFlow(AppUiState())
    val state = _state.asStateFlow()

    fun finishOnboarding() = viewModelScope.launch { c.settings.setOnboarded(true) }
    fun selectTool(type: ToolType) { _state.update { AppUiState(currentTool = type) } }
    fun setUris(uris: List<Uri>) { _state.update { it.copy(inputUris = uris, inputNames = uris.map { u -> getApplication<Application>().displayName(u) }, message = UiMessage.FilesSelected, error = null, result = null) } }
    fun setCompression(level: CompressionLevel) { _state.update { it.copy(compression = level) } }
    fun setPageRange(range: String) { _state.update { it.copy(pageRange = range) } }
    fun setPassword(pw: String) { _state.update { it.copy(password = pw) } }
    fun dismissMergeAd() { _state.update { it.copy(showMergeAd = false) } }
    fun resetTask() { _state.value = AppUiState(currentTool = state.value.currentTool) }
    fun setTheme(theme: String) = viewModelScope.launch { c.settings.setTheme(theme) }
    fun setLanguage(language: String) = viewModelScope.launch { c.settings.setLanguage(language) }

    fun process() = viewModelScope.launch {
        val s = state.value; val app = getApplication<Application>(); val plan = userPlan.value
        if (s.inputUris.isEmpty()) { _state.update { it.copy(error = UiMessage.NoFile) }; return@launch }
        _state.update { it.copy(processing = true, message = UiMessage.Processing, error = null, result = null, showMergeAd = false) }
        runCatching {
            when (s.currentTool) {
                ToolType.COMPRESS -> { val u=s.inputUris.first(); c.pdfEngine.compress(u, app.displayName(u), s.compression) }
                ToolType.MERGE -> c.pdfEngine.merge(s.inputUris)
                ToolType.SPLIT -> { val u=s.inputUris.first(); val pages=c.pdfEngine.pageCount(u); val parsed=parser.parse(s.pageRange, pages).getOrThrow(); c.pdfEngine.split(u, app.displayName(u), parsed, s.pageRange.ifBlank { "all" }) }
                ToolType.IMAGES_TO_PDF -> c.pdfEngine.imagesToPdf(s.inputUris, ImageToPdfOptions())
                ToolType.ROTATE -> { val u=s.inputUris.first(); c.pdfEngine.rotate(u, app.displayName(u), 90) }
                ToolType.PROTECT -> { val u=s.inputUris.first(); c.pdfEngine.protect(u, app.displayName(u), s.password, allowPrint = true, allowCopy = false) }
                ToolType.UNLOCK -> { val u=s.inputUris.first(); c.pdfEngine.unlock(u, app.displayName(u), s.password) }
                ToolType.PREVIEW, ToolType.PDF_TO_IMAGES, ToolType.HISTORY -> error(UiMessage.ToolRoadmap.name)
            }
        }.onSuccess { r ->
            c.history.add(HistoryItem(toolType=s.currentTool, displayName=r.fileName, createdAt=System.currentTimeMillis(), outputSizeBytes=r.outputSizeBytes, inputSizeBytes=r.inputSizeBytes, outputUriString=r.uri.toString()))
            _state.update { it.copy(processing=false, result=r, message=UiMessage.Done, showMergeAd = s.currentTool == ToolType.MERGE && plan == UserPlan.FREE) }
        }.onFailure { e ->
            _state.update { it.copy(processing=false, error = UiMessage.fromThrowable(e), message = null) }
        }
    }

    fun clearHistory() = viewModelScope.launch { c.history.clear() }
    fun restorePurchases() = c.billing.restore()
    fun billing() = c.billing
    fun mergeAds() = c.mergeAds
    fun handleIncoming(intent: Intent?) {
        val action = intent?.action ?: return
        val uris = mutableListOf<Uri>()
        if (action == Intent.ACTION_SEND) intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(uris::add)
        if (action == Intent.ACTION_SEND_MULTIPLE) intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let(uris::addAll)
        if (uris.isNotEmpty()) {
            val type = if (uris.size > 1 && intent.type?.startsWith("image/") == true) ToolType.IMAGES_TO_PDF else if (uris.size > 1) ToolType.MERGE else ToolType.COMPRESS
            _state.value = AppUiState(currentTool = type, inputUris = uris, inputNames = uris.map { getApplication<Application>().displayName(it) }, message = UiMessage.FilesSelected)
        }
    }
}

data class AppUiState(
    val currentTool: ToolType = ToolType.COMPRESS,
    val inputUris: List<Uri> = emptyList(),
    val inputNames: List<String> = emptyList(),
    val compression: CompressionLevel = CompressionLevel.MEDIUM,
    val pageRange: String = "1",
    val password: String = "",
    val processing: Boolean = false,
    val result: PdfOperationResult? = null,
    val message: UiMessage? = null,
    val error: UiMessage? = null,
    val showMergeAd: Boolean = false
)

enum class UiMessage { FilesSelected, Processing, Done, NoFile, ToolRoadmap, UnreadablePdf;
    companion object { fun fromThrowable(t: Throwable): UiMessage = runCatching { valueOf(t.message ?: "") }.getOrDefault(UnreadablePdf) }
}
