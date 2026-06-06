package de.pdfwerkzeugkasten

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.pdfwerkzeugkasten.data.ads.AdsConsentManager
import de.pdfwerkzeugkasten.domain.model.*
import de.pdfwerkzeugkasten.theme.PdfWerkzeugkastenTheme
import de.pdfwerkzeugkasten.util.humanSize
import de.pdfwerkzeugkasten.util.shareable
import java.io.File

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); AdsConsentManager(this).initialize(this); vm.handleIncoming(intent); setContent { App(vm) } }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); vm.handleIncoming(intent) }
}

@Composable fun App(vm: MainViewModel) {
    val theme by vm.theme.collectAsStateWithLifecycle(); val onboarded by vm.onboarded.collectAsStateWithLifecycle()
    PdfWerkzeugkastenTheme(theme) { if (!onboarded) Onboarding(vm) else MainScaffold(vm) }
}

@Composable fun Onboarding(vm: MainViewModel) {
    val pages = listOf(Triple("Deine PDF-Werkzeuge an einem Ort", "Komprimiere, kombiniere und konvertiere PDFs direkt auf deinem Gerät.", Icons.Default.PictureAsPdf), Triple("Privat & lokal", "Deine Dateien werden lokal verarbeitet und nicht auf Server hochgeladen.", Icons.Default.Lock), Triple("Schnell teilen", "Speichere fertige PDFs oder teile sie direkt per Mail, WhatsApp und Cloud.", Icons.Default.Share))
    var idx by remember { mutableIntStateOf(0) }; val p = pages[idx]
    Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Icon(p.third, null, modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(24.dp)); Text(p.first, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text(p.second, style = MaterialTheme.typography.bodyLarge); Spacer(Modifier.height(32.dp)); Button(onClick = { if (idx == pages.lastIndex) vm.finishOnboarding() else idx++ }, modifier = Modifier.fillMaxWidth()) { Text(if (idx == pages.lastIndex) "Loslegen" else "Weiter") } } }
}

@Composable fun MainScaffold(vm: MainViewModel) {
    var tab by remember { mutableStateOf("Home") }
    Scaffold(bottomBar = { NavigationBar { listOf("Home" to Icons.Default.Home, "Verlauf" to Icons.Default.History, "Pro" to Icons.Default.WorkspacePremium, "Einstellungen" to Icons.Default.Settings).forEach { (label, icon) -> NavigationBarItem(selected = tab == label, onClick = { tab = label }, icon = { Icon(icon, null) }, label = { Text(label) }) } } }) { padding -> Box(Modifier.padding(padding)) { when (tab) { "Home" -> HomeScreen(vm); "Verlauf" -> HistoryScreen(vm); "Pro" -> ProScreen(vm); else -> SettingsScreen(vm) } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HomeScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle(); val plan by vm.userPlan.collectAsStateWithLifecycle(); val context = LocalContext.current
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if (uris.isNotEmpty()) vm.setUris(uris) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris -> if (uris.isNotEmpty()) vm.setUris(uris) }
    Column(Modifier.fillMaxSize().padding(16.dp)) { Text("PDF Werkzeugkasten", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text("Deine PDFs bleiben auf deinem Gerät. Wir laden keine Dokumente auf Server hoch.", color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); SearchBar(query = "", onQueryChange = {}, onSearch = {}, active = false, onActiveChange = {}, placeholder = { Text("PDF-Tool suchen") }) {}; Spacer(Modifier.height(12.dp)); Text("Top-Actions", fontWeight = FontWeight.Bold); LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), modifier = Modifier.height(190.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(vm.tools.take(4)) { ToolCard(it) { vm.selectTool(it.id); if (it.id == ToolType.IMAGES_TO_PDF) imagePicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else pdfPicker.launch(arrayOf("application/pdf")) } } }; Spacer(Modifier.height(8.dp)); Text("Alle Tools", fontWeight = FontWeight.Bold); ToolFlow(vm, state, plan, context as Activity, pdfPicker, imagePicker) }
}

@Composable fun ToolFlow(vm: MainViewModel, state: AppUiState, plan: UserPlan, activity: Activity, pdfPicker: androidx.activity.compose.ManagedActivityResultLauncher<Array<String>, List<Uri>>, imagePicker: androidx.activity.compose.ManagedActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest, List<Uri>>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { item { LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.height(260.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(vm.tools) { ToolCard(it) { vm.selectTool(it.id); if (it.id == ToolType.HISTORY) return@ToolCard; if (it.id == ToolType.IMAGES_TO_PDF) imagePicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else pdfPicker.launch(arrayOf("application/pdf")) } } } }
        item { ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Aktueller Workflow: ${state.currentTool}", fontWeight = FontWeight.Bold); Text("Schritt 1: Datei auswählen · Schritt 2: Optionen · Schritt 3: Verarbeitung · Schritt 4: Ergebnis"); state.inputNames.forEach { Text("• $it") }; if (state.currentTool == ToolType.COMPRESS) Row { CompressionLevel.entries.forEach { FilterChip(selected = state.compression == it, onClick = { vm.setCompression(it) }, label = { Text(it.label) }); Spacer(Modifier.width(6.dp)) } }; if (state.currentTool == ToolType.SPLIT) OutlinedTextField(state.pageRange, vm::setPageRange, label = { Text("Seitenbereich, z. B. 1-3, 5") }); if (state.currentTool == ToolType.PROTECT || state.currentTool == ToolType.UNLOCK) OutlinedTextField(state.password, vm::setPassword, label = { Text("Passwort") }); Button(enabled = state.inputUris.isNotEmpty() && !state.processing, onClick = vm::process, modifier = Modifier.fillMaxWidth()) { Text(if (state.processing) "Verarbeitung läuft…" else "PDF verarbeiten") }; if (state.processing) LinearProgressIndicator(Modifier.fillMaxWidth()); state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }; Text("Plan: $plan · Free-Limits: 25 MB, 5 PDFs, 10 Bilder") } } }
        state.result?.let { r -> item { ResultCard(r) } }
    }
}

@Composable fun ToolCard(tool: PdfTool, onClick: () -> Unit) { ElevatedCard(onClick = onClick, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(iconFor(tool.id), null, tint = MaterialTheme.colorScheme.primary); Row(verticalAlignment = Alignment.CenterVertically) { Text(tool.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); if (tool.isProFeature) AssistChip(onClick = {}, label = { Text("Pro") }) }; Text(tool.description, style = MaterialTheme.typography.bodySmall) } } }
fun iconFor(type: ToolType): ImageVector = when (type) { ToolType.COMPRESS -> Icons.Default.Compress; ToolType.MERGE -> Icons.Default.CallMerge; ToolType.SPLIT -> Icons.Default.ContentCut; ToolType.IMAGES_TO_PDF -> Icons.Default.Image; ToolType.PDF_TO_IMAGES -> Icons.Default.Collections; ToolType.ROTATE -> Icons.Default.Rotate90DegreesCcw; ToolType.PROTECT -> Icons.Default.Lock; ToolType.UNLOCK -> Icons.Default.LockOpen; ToolType.PREVIEW -> Icons.Default.Visibility; ToolType.HISTORY -> Icons.Default.History }

@Composable fun ResultCard(r: PdfOperationResult) { val context = LocalContext.current; val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(r.mimeType)) { dest -> if (dest != null) context.contentResolver.openOutputStream(dest)?.use { out -> context.contentResolver.openInputStream(r.uri)?.use { it.copyTo(out) } } }
    ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Ergebnis erstellt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(r.fileName); Text("Vorher: ${r.inputSizeBytes.humanSize()} · Nachher: ${r.outputSizeBytes.humanSize()} · Ersparnis: ${r.savingsPercent}%"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button({ save.launch(r.fileName) }) { Text("Speichern") }; OutlinedButton({ val uri = context.shareable(r.uri); context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(r.mimeType).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Teilen")) }) { Text("Teilen") }; OutlinedButton({ val uri = context.shareable(r.uri); context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, r.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }) { Text("Öffnen") } }; Text("Anzeigeplatzhalter: Banner nur für Free-Nutzer auf Home/Ergebnis.", style = MaterialTheme.typography.bodySmall) } }
}

@Composable fun HistoryScreen(vm: MainViewModel) { val items by vm.history.collectAsStateWithLifecycle(); Column(Modifier.fillMaxSize().padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Verlauf", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(vm::clearHistory) { Text("Löschen") } }; Text("Der Verlauf speichert keine PDF-Inhalte, nur Metadaten."); LazyColumn { items(items) { item -> ListItem(headlineContent = { Text(item.displayName) }, supportingContent = { Text("${item.toolType} · ${item.outputSizeBytes.humanSize()}") }, leadingContent = { Icon(Icons.Default.History, null) }) } } } }
@Composable fun ProScreen(vm: MainViewModel) { val state by vm.billingState.collectAsStateWithLifecycle(); val activity = LocalContext.current as Activity; Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { Text("PDF Werkzeugkasten Pro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); listOf("Keine Werbung", "Keine Limits", "Batch-Verarbeitung", "Schnellere Workflows", "Premium-Design").forEach { Text("✓ $it") }; Button({ vm.billing().launchPurchase(activity) }, Modifier.fillMaxWidth()) { Text("Lifetime Pro freischalten") }; OutlinedButton(vm::restorePurchases, Modifier.fillMaxWidth()) { Text("Käufe wiederherstellen") }; Text(state) } }
@Composable fun SettingsScreen(vm: MainViewModel) { Column(Modifier.fillMaxSize().padding(16.dp)) { Text("Einstellungen", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); listOf("Design: System / Hell / Dunkel", "Standardspeicherort über Android-Speicherdialog", "Datenschutz", "Werbung & Zustimmung verwalten", "Feedback senden", "App bewerten", "Impressum", "Datenschutzrichtlinie", "Open-Source-Lizenzen").forEach { ListItem(headlineContent = { Text(it) }, leadingContent = { Icon(Icons.Default.Settings, null) }) }; Text(PRIVACY_POLICY, style = MaterialTheme.typography.bodySmall) } }
const val PRIVACY_POLICY = "Datenschutz: PDF Werkzeugkasten verarbeitet Dokumente lokal auf deinem Gerät und lädt keine PDFs auf eigene Server hoch. Werbung kann Ad-ID/Gerätekennungen über Google AdMob verarbeiten; EU-Nutzer erhalten Consent-Optionen über Google UMP. In-App-Käufe laufen über Google Play Billing. Crashlogs sind im MVP nicht aktiviert. Kontakt: privacy@example.com."
