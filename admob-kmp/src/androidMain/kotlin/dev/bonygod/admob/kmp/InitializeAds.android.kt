package dev.bonygod.admob.kmp

import android.content.Context
import com.google.android.gms.ads.MobileAds
import dev.bonygod.admob.kmp.config.getInterstitialAdUnitId

/**
 * Inicializa el SDK de Google Mobile Ads para Android y, si el intersticial
 * está activado en la configuración, lanza su precarga.
 *
 * No es `expect`/`actual`: la firma no puede ser igual en Android (necesita
 * un [Context]) que en iOS (no lo necesita, ver la extensión de `iosMain`),
 * así que cada plataforma declara su propia función.
 *
 * Llamar una única vez, lo antes posible, típicamente en
 * `Application.onCreate()`, después de haber llamado a
 * `AdMobKMP.configure(...)`.
 */
fun AdMobKMP.initializeAds(context: Context) {
    MobileAds.initialize(context) { }

    if (isInterstitialEnabled()) {
        InterstitialAdManager.preloadAd(context, getInterstitialAdUnitId())
    }
}
