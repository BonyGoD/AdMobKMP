package dev.bonygod.admob.kmp.config

/**
 * Configuración de AdMobKMP.
 *
 * Se construye una única vez, con los datos propios de la app que consume la
 * librería, y se entrega a `AdMobKMP.configure()` en el arranque (por ejemplo,
 * en `Application.onCreate()` en Android o en `init` de la App en iOS/Compose).
 * A partir de ahí, todos los componentes (`BannerAd`, `InterstitialAdScreen`,
 * `rememberInterstitialAd()`, etc.) la leen internamente: nadie más vuelve a
 * pasar un ad unit ID a mano.
 *
 * Ninguno de estos valores vive dentro de la librería. Los IDs de producción
 * son responsabilidad exclusiva de quien consume AdMobKMP — cada app tiene los
 * suyos, dados de alta en su propia cuenta de AdMob — y nunca deben publicarse
 * dentro de este artefacto.
 */
data class AdMobConfig(
    /**
     * Ad unit ID del banner en Android, tal y como aparece en la consola de
     * AdMob de la app consumidora. Cadena vacía si esa app no muestra banners
     * en Android o si va a arrancar con [useTestAds].
     */
    val androidBannerId: String = "",

    /**
     * Ad unit ID del intersticial en Android. Cadena vacía si la app no usa
     * intersticial en Android, o mientras se prueba con [useTestAds].
     */
    val androidInterstitialId: String = "",

    /**
     * Ad unit ID del banner en iOS. Cadena vacía si la app no muestra banners
     * en iOS, o mientras se prueba con [useTestAds].
     */
    val iosBannerId: String = "",

    /**
     * Ad unit ID del intersticial en iOS. Cadena vacía si la app no usa
     * intersticial en iOS, o mientras se prueba con [useTestAds].
     */
    val iosInterstitialId: String = "",

    /**
     * Si es `true`, la librería ignora los IDs de producción anteriores y
     * sirve siempre los IDs de prueba oficiales de Google (los mismos para
     * cualquier app, públicos y documentados por AdMob). Permite integrar y
     * probar la librería sin dar de alta ad units todavía.
     */
    val useTestAds: Boolean = false,

    /**
     * Interruptor general del intersticial. En `false`, la librería no
     * precarga ni muestra ningún intersticial: `InterstitialAdScreen` llama a
     * `onFinished` de inmediato y `rememberInterstitialAd()` no hace ninguna
     * petición de red. El banner no depende de este flag.
     */
    val interstitialEnabled: Boolean = true,
)
