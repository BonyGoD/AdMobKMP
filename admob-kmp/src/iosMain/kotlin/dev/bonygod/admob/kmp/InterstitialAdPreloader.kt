package dev.bonygod.admob.kmp

import dev.bonygod.admob.kmp.config.getInterstitialAdUnitId
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

object InterstitialAdPreloader {

    /**
     * Obtiene el Ad Unit ID correcto para iOS (test o producción), resuelto
     * por [AdMobKMP] a partir de la configuración dada en `AdMobKMP.configure()`.
     */
    fun getAdUnitId(): String {
        return AdMobKMP.getInterstitialAdUnitId()
    }

    /**
     * Expone a Swift el interruptor de configuración, que vive en commonMain
     * (`AdMobConfig.interstitialEnabled`, leído a través de [AdMobKMP]).
     * Swift no puede leer una `data class` de Kotlin directamente, así que se
     * consulta por aquí para que el flag siga siendo único para las dos
     * plataformas.
     */
    fun isInterstitialEnabled(): Boolean {
        return AdMobKMP.isInterstitialEnabled()
    }

    /**
     * Inicia la precarga del anuncio intersticial desde Kotlin.
     * Esta función debe ser llamada desde Swift al iniciar la app.
     *
     * Si el intersticial está desactivado en la configuración, no hace nada:
     * el flag se comprueba aquí, y no solo en quien llama, porque esta función
     * es un punto de entrada público que Swift invoca directamente en el
     * arranque. Sin esta guarda, una app con `interstitialEnabled = false`
     * seguiría pidiendo la precarga en iOS.
     */
    @OptIn(ExperimentalForeignApi::class)
    fun preloadInterstitial() {
        if (!AdMobKMP.isInterstitialEnabled()) {
            println("⏭️ [iOS-Kotlin] Intersticial desactivado, no se precarga")
            return
        }

        NSNotificationCenter.defaultCenter.postNotificationName(
            AdNotifications.PRELOAD_REQUESTED,
            `object` = null,
            userInfo = mapOf("adUnitId" to getAdUnitId())
        )
        println("🟡 [iOS-Kotlin] Preload requested for: ${getAdUnitId()}")
    }
}
