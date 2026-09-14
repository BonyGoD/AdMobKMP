package dev.bonygod.admob.kmp.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import dev.bonygod.admob.kmp.AdMobKMP
import dev.bonygod.admob.kmp.config.getBannerAdUnitId

@SuppressLint("MissingPermission")
@Composable
actual fun BannerAd(
    modifier: Modifier,
    adUnitId: String?,
    onAdLoaded: () -> Unit,
    onAdFailedToLoad: (String) -> Unit
) {
    val context = LocalContext.current
    val resolvedAdUnitId = adUnitId ?: AdMobKMP.getBannerAdUnitId()
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                // Usar un banner adaptativo que ocupe todo el ancho
                val display = context.resources.displayMetrics
                val adWidthPixels = display.widthPixels.toFloat()
                val density = display.density
                val adWidth = (adWidthPixels / density).toInt()

                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth))
                this.adUnitId = resolvedAdUnitId

                adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdLoaded() {
                        onAdLoaded()
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        onAdFailedToLoad(error.message)
                    }
                }

                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
