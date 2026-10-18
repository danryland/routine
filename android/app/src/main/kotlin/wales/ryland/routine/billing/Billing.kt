package wales.ryland.routine.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import wales.ryland.routine.data.AppSettings
import wales.ryland.routine.ui.TipOption

/**
 * The one-time unlock and the tip jar, via Google Play Billing.
 *
 * Product IDs must match the Play Console (see docs/launch-checklist.md).
 */
class Billing(context: Context, private val settings: AppSettings) : PurchasesUpdatedListener {
    data class State(
        /** Formatted price of the unlock, or null if Play can't be reached. */
        val unlockPrice: String? = null,
        val tips: List<TipOption> = emptyList(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _thanks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires after a tip goes through. */
    val thanks: SharedFlow<Unit> = _thanks

    private var products: Map<String, ProductDetails> = emptyMap()

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Connects (if needed) and refreshes prices and purchases. Safe to call often. */
    fun refresh() {
        if (client.isReady) {
            queryProducts()
            queryPurchases()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    queryProducts()
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    /** Opens Play's purchase sheet. Returns false if the product isn't available. */
    fun buy(activity: Activity, productId: String): Boolean {
        val details = products[productId] ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()))
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingResponseCode.OK) {
            purchases.orEmpty().forEach(::handle)
        }
    }

    private fun queryProducts() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                (listOf(UNLOCK) + TIPS).map {
                    QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(ProductType.INAPP).build()
                },
            )
            .build()
        client.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingResponseCode.OK) return@queryProductDetailsAsync
            products = details.productDetailsList.associateBy { it.productId }
            _state.update {
                State(
                    unlockPrice = products[UNLOCK]?.oneTimePurchaseOfferDetails?.formattedPrice,
                    tips = TIPS.mapNotNull { id ->
                        products[id]?.let { TipOption(id, it.name, it.oneTimePurchaseOfferDetails?.formattedPrice ?: "") }
                    },
                )
            }
        }
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = purchases.any { UNLOCK in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            settings.setPurchasedUnlock(owned)
            purchases.forEach(::handle)
        }
    }

    private fun handle(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (UNLOCK in purchase.products) {
            settings.setPurchasedUnlock(true)
            // Unacknowledged purchases are refunded after three days.
            if (!purchase.isAcknowledged) {
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) {}
            }
        }
        if (purchase.products.any { it in TIPS }) {
            // Tips are consumed so they can be given again.
            client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { result, _ ->
                if (result.responseCode == BillingResponseCode.OK) _thanks.tryEmit(Unit)
            }
        }
    }

    companion object {
        const val UNLOCK = "wales.ryland.routine.unlock"
        val TIPS = listOf("wales.ryland.routine.tip.small", "wales.ryland.routine.tip.medium", "wales.ryland.routine.tip.large")
    }
}
