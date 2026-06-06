package de.pdfwerkzeugkasten.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import de.pdfwerkzeugkasten.data.settings.SettingsRepository
import de.pdfwerkzeugkasten.domain.model.UserPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingRepository(private val context: Context, private val settings: SettingsRepository) : PurchasesUpdatedListener {
    private val _state = MutableStateFlow("Bereit")
    val state = _state.asStateFlow()
    private val client = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
    fun connect() { if (!client.isReady) client.startConnection(object : BillingClientStateListener { override fun onBillingServiceDisconnected() { _state.value = "Kaufprüfung offline – letzter Pro-Status bleibt erhalten." }; override fun onBillingSetupFinished(r: BillingResult) { _state.value = if (r.responseCode == BillingClient.BillingResponseCode.OK) "Google Play Billing verbunden" else "Kauf konnte nicht geprüft werden" } }) }
    fun launchPurchase(activity: Activity) { connect(); _state.value = "Lifetime Pro bitte im Play Console Produkt pdf_toolbox_pro konfigurieren." }
    suspend fun restore() { settings.setPlan(UserPlan.FREE); _state.value = "Wiederherstellung angefragt. In dieser MVP-Demo wird kein externer Kauf simuliert." }
    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) { _state.value = when (result.responseCode) { BillingClient.BillingResponseCode.OK -> "Kauf erfolgreich"; BillingClient.BillingResponseCode.USER_CANCELED -> "Kauf abgebrochen"; BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "Pro bereits aktiv"; else -> "Kauf konnte nicht geprüft werden" } }
}
