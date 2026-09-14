package dev.bonygod.admob.kmp.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.bonygod.admob.kmp.AdMobKMP
import dev.bonygod.admob.kmp.InterstitialAdManager
import dev.bonygod.admob.kmp.config.getInterstitialAdUnitId

private class AndroidInterstitialAdController(
    private val activity: Activity?,
) : InterstitialAdController {

    override fun preload() {
        if (!AdMobKMP.isInterstitialEnabled()) return
        val context = activity ?: return
        InterstitialAdManager.preloadAd(context, AdMobKMP.getInterstitialAdUnitId())
    }

    override fun isReady(): Boolean {
        if (!AdMobKMP.isInterstitialEnabled()) return false
        return InterstitialAdManager.isAdReady()
    }

    override fun show(onDismissed: () -> Unit) {
        if (!AdMobKMP.isInterstitialEnabled()) {
            onDismissed()
            return
        }

        val currentActivity = activity
        if (currentActivity == null) {
            android.util.Log.e("InterstitialAdController", "❌ Context is not an Activity, cannot show interstitial")
            onDismissed()
            return
        }

        InterstitialAdManager.showAd(
            activity = currentActivity,
            onAdShown = {},
            onAdDismissed = onDismissed,
            onAdFailedToShow = { error ->
                android.util.Log.e("InterstitialAdController", "❌ Failed to show ad: $error")
                onDismissed()
            }
        )
    }
}

@Composable
actual fun rememberInterstitialAd(): InterstitialAdController {
    val context = LocalContext.current
    return remember(context) { AndroidInterstitialAdController(context as? Activity) }
}
