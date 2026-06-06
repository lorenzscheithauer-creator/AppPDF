package de.pdfwerkzeugkasten.data.ads

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

interface MergeAdService {
    fun preload(activity: Activity)
    fun showAfterSuccessfulMerge(activity: Activity, onFinished: () -> Unit)
}

class AdMobMergeAdService : MergeAdService {
    private var interstitial: InterstitialAd? = null
    private var loading = false

    override fun preload(activity: Activity) {
        if (interstitial != null || loading) return
        loading = true
        InterstitialAd.load(
            activity,
            DEBUG_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { loading = false; interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { loading = false; interstitial = null }
            }
        )
    }

    override fun showAfterSuccessfulMerge(activity: Activity, onFinished: () -> Unit) {
        val ad = interstitial
        if (ad == null) { preload(activity); onFinished(); return }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { onFinished(); preload(activity) }
            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) { onFinished(); preload(activity) }
        }
        ad.show(activity)
    }

    companion object { const val DEBUG_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712" }
}
