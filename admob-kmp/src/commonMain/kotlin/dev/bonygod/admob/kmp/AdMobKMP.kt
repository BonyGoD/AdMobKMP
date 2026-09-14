package dev.bonygod.admob.kmp

import dev.bonygod.admob.kmp.config.AdMobConfig

/**
 * Punto de entrada público de AdMobKMP.
 *
 * La librería no lee `BuildConfig` de nadie ni depende de ningún framework de
 * inyección de dependencias: toda su configuración vive en un único objeto en
 * memoria, cargado una vez al arrancar la app consumidora con [configure].
 * Todos los componentes de la librería (`BannerAd`, `InterstitialAdScreen`,
 * `rememberInterstitialAd()`...) leen de aquí.
 *
 * Ejemplo de uso en el arranque de la app:
 * ```kotlin
 * AdMobKMP.configure(
 *     AdMobConfig(
 *         androidBannerId = TU_ANDROID_BANNER_AD_UNIT_ID,
 *         androidInterstitialId = TU_ANDROID_INTERSTITIAL_AD_UNIT_ID,
 *         iosBannerId = TU_IOS_BANNER_AD_UNIT_ID,
 *         iosInterstitialId = TU_IOS_INTERSTITIAL_AD_UNIT_ID,
 *         useTestAds = BuildConfig.DEBUG,
 *         interstitialEnabled = true,
 *     )
 * )
 * ```
 */
object AdMobKMP {

    private var currentConfig: AdMobConfig = AdMobConfig()

    /**
     * Da de alta la configuración de la app consumidora. Debe llamarse una
     * sola vez, lo antes posible en el arranque, antes de que se muestre
     * ningún componente de la librería.
     */
    fun configure(config: AdMobConfig) {
        currentConfig = config
    }

    /**
     * Configuración activa. Si nadie llamó todavía a [configure], devuelve
     * `AdMobConfig()` con sus valores por defecto en vez de lanzar una
     * excepción: una librería de anuncios no debe tumbar la app de quien la
     * consume solo por no estar configurada aún.
     */
    fun config(): AdMobConfig = currentConfig

    /**
     * Atajo sobre `config().interstitialEnabled`. Se mantiene como método
     * propio porque el lado Swift lo necesita a través de
     * `InterstitialAdPreloader`, y desde Swift no se puede leer una
     * `data class` de Kotlin tan cómodamente como llamar a una función.
     */
    fun isInterstitialEnabled(): Boolean = config().interstitialEnabled
}
