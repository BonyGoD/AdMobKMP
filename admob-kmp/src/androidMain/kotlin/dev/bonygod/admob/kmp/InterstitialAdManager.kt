package dev.bonygod.admob.kmp

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dev.bonygod.admob.kmp.config.getInterstitialAdUnitId

object InterstitialAdManager {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    fun preloadAd(context: Context, adUnitId: String) {
        // El flag se comprueba aquí, y no solo en quien llama, porque este es
        // un punto de entrada público y además se reentra a sí mismo tras
        // cerrarse o fallar un anuncio (ver showAd). Con el intersticial
        // desactivado no debe salir ninguna petición de red.
        if (!AdMobKMP.isInterstitialEnabled()) {
            android.util.Log.d("InterstitialAdManager", "⏭️ Intersticial desactivado, no se precarga")
            return
        }

        if (interstitialAd != null || isLoading) {
            android.util.Log.d("InterstitialAdManager", "⚠️ Ad already loaded or loading")
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()
        android.util.Log.d("InterstitialAdManager", "🟡 Preloading ad...")

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    android.util.Log.d("InterstitialAdManager", "✅ Ad preloaded successfully")
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.e("InterstitialAdManager", "❌ Failed to preload ad: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        onAdShown: () -> Unit,
        onAdDismissed: () -> Unit,
        onAdFailedToShow: (String) -> Unit
    ) {
        val ad = interstitialAd
        if (ad != null) {
            android.util.Log.d("InterstitialAdManager", "🟢 Showing preloaded ad")
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    android.util.Log.d("InterstitialAdManager", "✅ Ad showed")
                    onAdShown()
                }

                override fun onAdDismissedFullScreenContent() {
                    android.util.Log.d("InterstitialAdManager", "👋 Ad dismissed")
                    interstitialAd = null
                    onAdDismissed()
                    // Precargar el siguiente anuncio
                    preloadAd(activity, AdMobKMP.getInterstitialAdUnitId())
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    android.util.Log.e("InterstitialAdManager", "❌ Failed to show ad: ${error.message}")
                    interstitialAd = null
                    onAdFailedToShow(error.message)
                    // Precargar de nuevo
                    preloadAd(activity, AdMobKMP.getInterstitialAdUnitId())
                }
            }
            ad.show(activity)
        } else {
            android.util.Log.e("InterstitialAdManager", "❌ Ad not ready")
            onAdFailedToShow("Ad not preloaded")
        }
    }

    fun isAdReady(): Boolean = interstitialAd != null
}
