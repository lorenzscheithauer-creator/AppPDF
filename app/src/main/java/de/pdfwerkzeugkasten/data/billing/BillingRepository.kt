package de.pdfwerkzeugkasten.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import de.pdfwerkzeugkasten.data.settings.SettingsRepository
import de.pdfwerkzeugkasten.domain.model.UserPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingRepository(private val context: Context, private val settings: SettingsRepository) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val _state = MutableStateFlow(BillingUiState())
    val state = _state.asStateFlow()
    private var productDetails: ProductDetails? = null
    private val client = BillingClient.newBuilder(context).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).setListener(this).build()

    fun connect() {
        if (client.isReady) { loadProductDetails(); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() { _state.value = _state.value.copy(message = BillingMessage.Offline) }
            override fun onBillingSetupFinished(result: BillingResult) { if (result.responseCode == BillingClient.BillingResponseCode.OK) loadProductDetails() else _state.value = _state.value.copy(message = BillingMessage.ProductMissing) }
        })
    }

    private fun loadProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { _, products ->
            productDetails = products.firstOrNull()
            _state.value = BillingUiState(productLoaded = productDetails != null, message = if (productDetails == null) BillingMessage.ProductMissing else BillingMessage.Ready)
        }
    }

    fun launchPurchase(activity: Activity) {
        connect()
        val details = productDetails
        if (details == null) { _state.value = _state.value.copy(message = BillingMessage.ProductMissing); return }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())).build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) _state.value = _state.value.copy(message = BillingMessage.StartFailed)
    }

    fun restore() {
        connect()
        if (!client.isReady) { _state.value = _state.value.copy(message = BillingMessage.Offline); return }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { result, purchases ->
            val owned = result.responseCode == BillingClient.BillingResponseCode.OK && purchases.any { it.products.contains(PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            scope.launch { settings.setPlan(if (owned) UserPlan.PRO else UserPlan.FREE) }
            _state.value = _state.value.copy(message = if (owned) BillingMessage.Owned else BillingMessage.NoneFound)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::handlePurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.value = _state.value.copy(message = BillingMessage.Cancelled)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
            else -> _state.value = _state.value.copy(message = BillingMessage.CheckFailed)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (!purchase.products.contains(PRODUCT_ID)) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) {}
                scope.launch { settings.setPlan(UserPlan.PRO) }
                _state.value = _state.value.copy(message = BillingMessage.Success)
            }
            Purchase.PurchaseState.PENDING -> _state.value = _state.value.copy(message = BillingMessage.Pending)
            else -> _state.value = _state.value.copy(message = BillingMessage.CheckFailed)
        }
    }

    companion object { const val PRODUCT_ID = "pdf_toolbox_pro" }
}

data class BillingUiState(val productLoaded: Boolean = false, val message: BillingMessage = BillingMessage.Ready)
enum class BillingMessage { Ready, Offline, ProductMissing, StartFailed, CheckFailed, Cancelled, Pending, Success, Owned, NoneFound }
