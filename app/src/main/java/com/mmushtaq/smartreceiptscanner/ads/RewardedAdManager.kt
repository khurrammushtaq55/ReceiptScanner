// RewardedAdManager.kt
package com.mmushtaq.smartreceiptscanner.ads

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(
    private val activity: Activity,
    private val adUnitId: String
) {
    private var ad: RewardedAd? = null

    fun load(onLoaded: (() -> Unit)? = null, onFailed: (() -> Unit)? = null) {
        RewardedAd.load(activity, adUnitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    ad = rewardedAd; onLoaded?.invoke()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null; onFailed?.invoke()
                }
            })
    }

    fun show(onReward: (amount: Int, type: String) -> Unit, onDismiss: (() -> Unit)? = null) {
        val current = ad ?: return onDismiss?.invoke() ?: Unit
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { ad = null; onDismiss?.invoke() }
            override fun onAdFailedToShowFullScreenContent(p0: AdError) { ad = null; onDismiss?.invoke() }
        }
        current.show(activity) { rewardItem ->
            onReward(rewardItem.amount, rewardItem.type)
        }
    }

    val isReady get() = ad != null
}

@Composable
fun rememberRewardedManager(testMode: Boolean, realUnitId: String): RewardedAdManager {
    val ctx = LocalContext.current
    val act = ctx as Activity
    val id = if (testMode)
        ctx.getString(com.mmushtaq.smartreceiptscanner.R.string.admob_test_rewarded)
    else realUnitId
    return remember { RewardedAdManager(act, id) }
}
