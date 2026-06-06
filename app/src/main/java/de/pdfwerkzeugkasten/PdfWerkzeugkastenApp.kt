package de.pdfwerkzeugkasten

import android.app.Application
import de.pdfwerkzeugkasten.data.ads.AdMobMergeAdService
import de.pdfwerkzeugkasten.data.billing.BillingRepository
import de.pdfwerkzeugkasten.data.history.HistoryDatabase
import de.pdfwerkzeugkasten.data.history.HistoryRepository
import de.pdfwerkzeugkasten.data.pdf.PdfBoxEngine
import de.pdfwerkzeugkasten.data.settings.SettingsRepository

class PdfWerkzeugkastenApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() { super.onCreate(); container = AppContainer(this) }
}
class AppContainer(app: Application) { val settings = SettingsRepository(app); val history = HistoryRepository(HistoryDatabase.create(app).dao()); val pdfEngine = PdfBoxEngine(app); val billing = BillingRepository(app, settings); val mergeAds = AdMobMergeAdService() }
