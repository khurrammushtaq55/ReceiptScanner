// BannerAd.kt
package com.mmushtaq.smartreceiptscanner.ads

import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.mmushtaq.smartreceiptscanner.ads.AdsSetting.USE_TEST_ADS

@Composable
fun BannerAd(
    adId: String = stringResource(com.mmushtaq.smartreceiptscanner.R.string.admob_banner_id),
    useTestId: Boolean = USE_TEST_ADS
) {
    val context = LocalContext.current
    val realId = if (useTestId) {
        context.getString(com.mmushtaq.smartreceiptscanner.R.string.admob_test_banner)
    } else adId

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = realId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
