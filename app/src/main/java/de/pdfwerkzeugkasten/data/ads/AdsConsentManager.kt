package de.pdfwerkzeugkasten.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class AdsConsentManager(private val context: Context) {
    fun initialize(activity: Activity, onReady: () -> Unit = {}) {
        val params = ConsentRequestParameters.Builder().build()
        val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
        info.requestConsentInfoUpdate(activity, params, { if (info.isConsentFormAvailable) UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { MobileAds.initialize(context); onReady() } else { MobileAds.initialize(context); onReady() } }, { MobileAds.initialize(context); onReady() })
    }
}
