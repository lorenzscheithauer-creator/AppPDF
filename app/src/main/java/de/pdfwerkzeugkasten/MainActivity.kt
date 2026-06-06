package de.pdfwerkzeugkasten

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.pdfwerkzeugkasten.data.ads.AdsConsentManager
import de.pdfwerkzeugkasten.data.billing.BillingMessage
import de.pdfwerkzeugkasten.domain.model.*
import de.pdfwerkzeugkasten.theme.PdfWerkzeugkastenTheme
import de.pdfwerkzeugkasten.util.LocaleUtil
import de.pdfwerkzeugkasten.util.humanSize
import de.pdfwerkzeugkasten.util.shareable

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdsConsentManager(this).initialize(this)
        vm.billing().connect()
        vm.handleIncoming(intent)
        setContent { PdfApp(vm) }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); vm.handleIncoming(intent) }
}

@Composable private fun PdfApp(vm: MainViewModel) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val onboarded by vm.onboarded.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity
    LaunchedEffect(language) { LocaleUtil.apply(activity, language) }
    PdfWerkzeugkastenTheme(theme) { if (onboarded) MainScaffold(vm) else Onboarding(vm) }
}

@Composable private fun Onboarding(vm: MainViewModel) {
    val pages = listOf(
        OnboardingPage(stringResource(R.string.onboarding_title_1), stringResource(R.string.onboarding_body_1), Icons.Default.PictureAsPdf),
        OnboardingPage(stringResource(R.string.onboarding_title_2), stringResource(R.string.onboarding_body_2), Icons.Default.Lock),
        OnboardingPage(stringResource(R.string.onboarding_title_3), stringResource(R.string.onboarding_body_3), Icons.Default.Share)
    )
    var index by remember { mutableIntStateOf(0) }
    val page = pages[index]
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(page.icon, null, modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text(page.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(page.body, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(32.dp))
            Button(onClick = { if (index == pages.lastIndex) vm.finishOnboarding() else index++ }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (index == pages.lastIndex) stringResource(R.string.action_start) else stringResource(R.string.action_continue))
            }
        }
    }
}
private data class OnboardingPage(val title: String, val body: String, val icon: ImageVector)

@Composable private fun MainScaffold(vm: MainViewModel) {
    var tab by remember { mutableStateOf(BottomTab.Home) }
    Scaffold(bottomBar = {
        NavigationBar {
            BottomTab.entries.forEach { item ->
                NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(item.icon, null) }, label = { Text(stringResource(item.labelRes)) })
            }
        }
    }) { padding ->
        Box(Modifier.padding(padding)) { when (tab) { BottomTab.Home -> HomeScreen(vm); BottomTab.History -> HistoryScreen(vm); BottomTab.Pro -> ProScreen(vm); BottomTab.Settings -> SettingsScreen(vm) } }
    }
}
private enum class BottomTab(val labelRes: Int, val icon: ImageVector) { Home(R.string.nav_home, Icons.Default.Home), History(R.string.nav_history, Icons.Default.History), Pro(R.string.nav_pro, Icons.Default.WorkspacePremium), Settings(R.string.nav_settings, Icons.Default.Settings) }

@Composable private fun HomeScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val plan by vm.userPlan.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val tools = localizedTools()
    val filtered = remember(query, tools) { ToolSearchFilter.filter(query, tools.map { it.search }).map { it.id } }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { if (it.isNotEmpty()) vm.setUris(it) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { if (it.isNotEmpty()) vm.setUris(it) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { HomeHeader(query, onQueryChange = { query = it }) }
        if (query.isBlank()) item { ToolSection(stringResource(R.string.home_top_actions), tools.take(4), vm, pdfPicker, imagePicker) }
        item { Text(stringResource(R.string.home_all_tools), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (filtered.isEmpty()) item { EmptySearchCard() } else items(tools.filter { it.type in filtered }, key = { it.type.name }) { ToolRow(it) { launchTool(it.type, vm, pdfPicker, imagePicker) } }
        item { WorkflowCard(vm, state, plan) }
        state.result?.let { item { ResultCard(it, vm) } }
        if (state.showMergeAd && plan == UserPlan.FREE) item { MergeAdCard(vm) }
    }
}

@Composable private fun HomeHeader(query: String, onQueryChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        AssistChip(onClick = {}, label = { Text(stringResource(R.string.home_trust)) }, leadingIcon = { Icon(Icons.Default.Security, null) })
        OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (query.isNotEmpty()) IconButton({ onQueryChange("") }) { Icon(Icons.Default.Close, null) } }, placeholder = { Text(stringResource(R.string.home_search_hint)) })
    }
}

@Composable private fun ToolSection(title: String, tools: List<ToolUi>, vm: MainViewModel, pdfPicker: ManagedActivityResultLauncher<Array<String>, List<Uri>>, imagePicker: ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); tools.forEach { ToolRow(it) { launchTool(it.type, vm, pdfPicker, imagePicker) } } }
}

@Composable private fun ToolRow(tool: ToolUi, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp)) { Box(contentAlignment = Alignment.Center) { Icon(iconFor(tool.type), null, tint = MaterialTheme.colorScheme.onPrimaryContainer) } }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(tool.title, fontWeight = FontWeight.Bold); Text(tool.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (tool.isPro) AssistChip(onClick = {}, label = { Text(stringResource(R.string.nav_pro)) })
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable private fun WorkflowCard(vm: MainViewModel, state: AppUiState, plan: UserPlan) {
    val title = localizedTools().firstOrNull { it.type == state.currentTool }?.title.orEmpty()
    ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.workflow_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(if (state.currentTool == ToolType.IMAGES_TO_PDF) stringResource(R.string.workflow_select_images) else stringResource(R.string.workflow_select_pdf))
            SelectedFiles(state.inputNames)
            ToolOptions(vm, state)
            if (state.processing) ProcessingState() else Button(enabled = state.inputUris.isNotEmpty(), onClick = vm::process, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.workflow_process)) }
            state.error?.let { ErrorState(it, onRetry = vm::process, onNew = vm::resetTask) }
        }
    }
}

@Composable private fun SelectedFiles(names: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.workflow_selected_files), fontWeight = FontWeight.SemiBold)
        if (names.isEmpty()) { Text(stringResource(R.string.workflow_no_files)); Text(stringResource(R.string.workflow_no_files_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) } else names.forEachIndexed { index, name -> Text("${index + 1}. $name") }
    }
}

@Composable private fun ToolOptions(vm: MainViewModel, state: AppUiState) {
    if (state.currentTool == ToolType.COMPRESS) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { CompressionLevel.entries.forEach { level -> FilterChip(selected = state.compression == level, onClick = { vm.setCompression(level) }, label = { Text(level.labelText()) }) } }
    if (state.currentTool == ToolType.SPLIT) OutlinedTextField(state.pageRange, vm::setPageRange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.workflow_page_range)) })
    if (state.currentTool == ToolType.PROTECT || state.currentTool == ToolType.UNLOCK) OutlinedTextField(state.password, vm::setPassword, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.workflow_password)) })
}

@Composable private fun CompressionLevel.labelText(): String = when (this) { CompressionLevel.LIGHT -> stringResource(R.string.compression_light); CompressionLevel.MEDIUM -> stringResource(R.string.compression_medium); CompressionLevel.STRONG -> stringResource(R.string.compression_strong) }

@Composable private fun ProcessingState() { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.workflow_processing), color = MaterialTheme.colorScheme.primary) } }
@Composable private fun ErrorState(error: UiMessage, onRetry: () -> Unit, onNew: () -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(messageText(error), color = MaterialTheme.colorScheme.onErrorContainer); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onRetry) { Text(stringResource(R.string.workflow_retry)) }; OutlinedButton(onNew) { Text(stringResource(R.string.workflow_new)) } } } } }
@Composable private fun EmptySearchCard() { ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(stringResource(R.string.home_empty_title), fontWeight = FontWeight.Bold); Text(stringResource(R.string.home_empty_body), color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun ResultCard(result: PdfOperationResult, vm: MainViewModel) {
    val context = LocalContext.current
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(result.mimeType)) { dest -> if (dest != null) context.contentResolver.openOutputStream(dest)?.use { out -> context.contentResolver.openInputStream(result.uri)?.use { it.copyTo(out) } } }
    ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp)); Text(stringResource(R.string.result_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(stringResource(R.string.result_subtitle)); Text(result.fileName); Text(stringResource(R.string.result_before_after, result.inputSizeBytes.humanSize(), result.outputSizeBytes.humanSize(), result.savingsPercent)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button({ save.launch(result.fileName) }, Modifier.weight(1f)) { Text(stringResource(R.string.action_save)) }; OutlinedButton({ share(context as Activity, result) }, Modifier.weight(1f)) { Text(stringResource(R.string.action_share)) } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ open(context as Activity, result) }, Modifier.weight(1f)) { Text(stringResource(R.string.action_open)) }; OutlinedButton(vm::resetTask, Modifier.weight(1f)) { Text(stringResource(R.string.workflow_new)) } } } }
}
private fun share(activity: Activity, r: PdfOperationResult) { val uri = activity.shareable(r.uri); runCatching { activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(r.mimeType).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), activity.getString(R.string.action_share))) } }
private fun open(activity: Activity, r: PdfOperationResult) { val uri = activity.shareable(r.uri); runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, r.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } }

@Composable private fun MergeAdCard(vm: MainViewModel) {
    val activity = LocalContext.current as Activity
    LaunchedEffect(Unit) { vm.mergeAds().showAfterSuccessfulMerge(activity) { vm.dismissMergeAd() } }
    ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(stringResource(R.string.ad_debug_title), fontWeight = FontWeight.Bold); Text(stringResource(R.string.ad_debug_body)); TextButton(vm::dismissMergeAd) { Text(stringResource(R.string.action_continue)) } } }
}

@Composable private fun HistoryScreen(vm: MainViewModel) { val items by vm.history.collectAsStateWithLifecycle(); LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Row(verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(vm::clearHistory) { Text(stringResource(R.string.action_clear)) } }; Text(stringResource(R.string.history_privacy), color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (items.isEmpty()) item { EmptyText(stringResource(R.string.history_empty)) } else items(items) { item -> ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) { ListItem(headlineContent = { Text(item.displayName) }, supportingContent = { Text("${item.toolType} · ${item.outputSizeBytes.humanSize()}") }, leadingContent = { Icon(Icons.Default.History, null) }) } } } }
@Composable private fun ProScreen(vm: MainViewModel) { val billing by vm.billingState.collectAsStateWithLifecycle(); val activity = LocalContext.current as Activity; LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(stringResource(R.string.pro_price), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }; items(listOf(R.string.pro_no_ads, R.string.pro_higher_limits, R.string.pro_all_premium)) { Text("✓ ${stringResource(it)}", style = MaterialTheme.typography.bodyLarge) }; item { Button({ vm.billing().launchPurchase(activity) }, Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.pro_unlock)) }; Spacer(Modifier.height(8.dp)); OutlinedButton(vm::restorePurchases, Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.pro_restore)) }; Text(billingMessageText(billing.message), color = MaterialTheme.colorScheme.onSurfaceVariant); if (!billing.productLoaded) Text(stringResource(R.string.pro_product_missing), color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun SettingsScreen(vm: MainViewModel) { val language by vm.language.collectAsStateWithLifecycle(); val theme by vm.theme.collectAsStateWithLifecycle(); val activity = LocalContext.current as Activity; LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }; item { SettingsGroup(stringResource(R.string.settings_language)) { LanguageOption(R.string.settings_language_de, LocaleUtil.GERMAN, language) { vm.setLanguage(it); LocaleUtil.apply(activity, it); activity.window.decorView.postDelayed({ activity.recreate() }, 250) }; LanguageOption(R.string.settings_language_en, LocaleUtil.ENGLISH, language) { vm.setLanguage(it); LocaleUtil.apply(activity, it); activity.window.decorView.postDelayed({ activity.recreate() }, 250) } } }; item { SettingsGroup(stringResource(R.string.settings_design)) { listOf(R.string.settings_design_system to "System", R.string.settings_design_light to "Hell", R.string.settings_design_dark to "Dunkel").forEach { (label, value) -> RadioLine(stringResource(label), theme == value) { vm.setTheme(value) } } } }; item { SettingsGroup(stringResource(R.string.settings_privacy)) { Text(stringResource(R.string.settings_privacy_text)) } }; item { SettingsGroup(stringResource(R.string.settings_ads_consent)) { Text(stringResource(R.string.home_trust)); Button({ AdsConsentManager(activity).initialize(activity) }) { Text(stringResource(R.string.settings_ads_consent)) } } }; item { SettingsGroup(stringResource(R.string.settings_pro_restore)) { OutlinedButton(vm::restorePurchases) { Text(stringResource(R.string.pro_restore)) } } }; item { SettingsGroup(stringResource(R.string.settings_app_info)) { Text(stringResource(R.string.settings_version)); Text(stringResource(R.string.app_name)) } } } }
@Composable private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) { ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(title, fontWeight = FontWeight.Bold); content() } } }
@Composable private fun LanguageOption(label: Int, value: String, selected: String, onClick: (String) -> Unit) = RadioLine(stringResource(label), selected == value) { onClick(value) }
@Composable private fun RadioLine(text: String, selected: Boolean, onClick: () -> Unit) { Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected, onClick); Text(text, Modifier.weight(1f)) } }
@Composable private fun EmptyText(text: String) { ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { Text(text, Modifier.padding(18.dp)) } }

@Composable private fun localizedTools(): List<ToolUi> = listOf(
    ToolUi(ToolType.COMPRESS, stringResource(R.string.tool_compress_title), stringResource(R.string.tool_compress_desc), stringResource(R.string.tool_compress_keywords)),
    ToolUi(ToolType.MERGE, stringResource(R.string.tool_merge_title), stringResource(R.string.tool_merge_desc), stringResource(R.string.tool_merge_keywords)),
    ToolUi(ToolType.IMAGES_TO_PDF, stringResource(R.string.tool_images_title), stringResource(R.string.tool_images_desc), stringResource(R.string.tool_images_keywords)),
    ToolUi(ToolType.SPLIT, stringResource(R.string.tool_split_title), stringResource(R.string.tool_split_desc), stringResource(R.string.tool_split_keywords)),
    ToolUi(ToolType.PDF_TO_IMAGES, stringResource(R.string.tool_pdf_images_title), stringResource(R.string.tool_pdf_images_desc), stringResource(R.string.tool_pdf_images_keywords), true),
    ToolUi(ToolType.ROTATE, stringResource(R.string.tool_rotate_title), stringResource(R.string.tool_rotate_desc), stringResource(R.string.tool_rotate_keywords)),
    ToolUi(ToolType.PROTECT, stringResource(R.string.tool_protect_title), stringResource(R.string.tool_protect_desc), stringResource(R.string.tool_protect_keywords)),
    ToolUi(ToolType.UNLOCK, stringResource(R.string.tool_unlock_title), stringResource(R.string.tool_unlock_desc), stringResource(R.string.tool_unlock_keywords)),
    ToolUi(ToolType.PREVIEW, stringResource(R.string.tool_preview_title), stringResource(R.string.tool_preview_desc), stringResource(R.string.tool_preview_keywords), true)
)
private data class ToolUi(val type: ToolType, val title: String, val description: String, val keywords: String, val isPro: Boolean = false) { val search = SearchableToolText(type, title, description, keywords) }
private fun launchTool(type: ToolType, vm: MainViewModel, pdfPicker: ManagedActivityResultLauncher<Array<String>, List<Uri>>, imagePicker: ManagedActivityResultLauncher<PickVisualMediaRequest, List<Uri>>) { vm.selectTool(type); if (type == ToolType.IMAGES_TO_PDF) imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else pdfPicker.launch(arrayOf("application/pdf")) }
private fun iconFor(type: ToolType): ImageVector = when (type) { ToolType.COMPRESS -> Icons.Default.Compress; ToolType.MERGE -> Icons.Default.CallMerge; ToolType.SPLIT -> Icons.Default.ContentCut; ToolType.IMAGES_TO_PDF -> Icons.Default.Image; ToolType.PDF_TO_IMAGES -> Icons.Default.Collections; ToolType.ROTATE -> Icons.Default.Rotate90DegreesCcw; ToolType.PROTECT -> Icons.Default.Lock; ToolType.UNLOCK -> Icons.Default.LockOpen; ToolType.PREVIEW -> Icons.Default.Visibility; ToolType.HISTORY -> Icons.Default.History }
@Composable private fun messageText(message: UiMessage): String = when (message) { UiMessage.NoFile -> stringResource(R.string.error_no_file); UiMessage.FileTooLarge -> stringResource(R.string.error_file_too_large); UiMessage.MergeLimit -> stringResource(R.string.error_merge_limit); UiMessage.ImageLimit -> stringResource(R.string.error_image_limit); UiMessage.UnreadablePdf -> stringResource(R.string.error_unreadable_pdf); UiMessage.ToolRoadmap -> stringResource(R.string.pro_all_premium); UiMessage.Processing -> stringResource(R.string.workflow_processing); UiMessage.Done -> stringResource(R.string.result_title); UiMessage.FilesSelected -> stringResource(R.string.workflow_selected_files) }

@Composable private fun billingMessageText(message: BillingMessage): String = when (message) { BillingMessage.Ready -> stringResource(R.string.billing_ready); BillingMessage.Offline -> stringResource(R.string.billing_offline); BillingMessage.ProductMissing -> stringResource(R.string.billing_product_missing); BillingMessage.StartFailed -> stringResource(R.string.billing_start_failed); BillingMessage.CheckFailed -> stringResource(R.string.billing_check_failed); BillingMessage.Cancelled -> stringResource(R.string.billing_cancelled); BillingMessage.Pending -> stringResource(R.string.billing_pending); BillingMessage.Success -> stringResource(R.string.billing_success); BillingMessage.Owned -> stringResource(R.string.billing_owned); BillingMessage.NoneFound -> stringResource(R.string.billing_none) }
