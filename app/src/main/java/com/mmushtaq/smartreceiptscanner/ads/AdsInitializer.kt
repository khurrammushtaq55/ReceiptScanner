// AdsInitializer.kt
package com.mmushtaq.smartreceiptscanner.ads

import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object AdsInitializer {
    fun init(activity: Activity, testDeviceIds: List<String> = emptyList()) {
        // (Optional) Set test device IDs for banner/interstitial loading logs
        if (testDeviceIds.isNotEmpty()) {
            val config = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(config)
        }

        // UMP Consent
        val params = ConsentRequestParameters.Builder().build()
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                // If required, load & show form
                if (consentInfo.isConsentFormAvailable) {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { /* noop */ }
                }
                // Once consent flow completes (or not needed), init MobileAds
                MobileAds.initialize(activity)
            },
            { // error
                MobileAds.initialize(activity) // still initialize to serve non-personalized if needed
            }
        )
    }
}
