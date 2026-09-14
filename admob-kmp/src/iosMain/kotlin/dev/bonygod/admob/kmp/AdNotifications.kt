package dev.bonygod.admob.kmp

/**
 * Nombres de `NSNotification` que forman el puente entre este módulo Kotlin
 * y el paquete Swift `AdMobKMPSwift` (Fase 5 del plan de migración).
 *
 * Kotlin/Native no puede enlazar el SDK de Google Mobile Ads para iOS por sí
 * mismo (eso lo hace el lado Swift), así que la comunicación entre las dos
 * mitades va por `NSNotificationCenter` en vez de por una llamada directa.
 *
 * **Contrato compartido, no automático.** Estos nombres son literales que
 * también existen, por separado, en el paquete Swift — no hay un tipo común
 * que los sincronice. Cambiar un valor aquí y no en Swift (o al revés) los
 * desincroniza en silencio: la notificación deja de emparejarse y el
 * componente correspondiente se queda esperando para siempre sin ningún
 * error visible. Cualquier cambio en esta tabla exige el mismo cambio en
 * `AdMobKMPSwift`.
 *
 * `PRELOAD_COMPLETED` y `PRELOAD_FAILED` son nombres nuevos: hoy nada los
 * emite todavía. Los necesita [dev.bonygod.admob.kmp.ui.rememberInterstitialAd]
 * para saber, de forma asíncrona pero fiable, cuándo terminó la precarga en
 * iOS — la Fase 5 tiene que hacer que el `AdPreloader.swift` fusionado los
 * publique al terminar `preloadAd(adUnitId:)`, con éxito o con error.
 */
internal object AdNotifications {

    // --- Intersticial: petición Kotlin -> Swift ---

    /** Pide a Swift que precargue un intersticial. `userInfo["adUnitId"]`. */
    const val PRELOAD_REQUESTED = "AdPreloaderPreloadRequested"

    /** Pide a Swift que muestre el intersticial ya precargado, si lo hay. */
    const val SHOW_REQUESTED = "AdPreloaderShowRequested"

    /** Pide a Swift que informe si hay un intersticial listo. */
    const val IS_READY_REQUESTED = "AdPreloaderIsReadyRequested"

    // --- Intersticial: respuesta Swift -> Kotlin ---

    /** Swift confirma que la precarga terminó con éxito. Nuevo (Fase 5). */
    const val PRELOAD_COMPLETED = "AdPreloaderPreloadCompleted"

    /** Swift confirma que la precarga falló. `userInfo["error"]`. Nuevo (Fase 5). */
    const val PRELOAD_FAILED = "AdPreloaderPreloadFailed"

    /** Swift confirma que el intersticial se mostró en pantalla. */
    const val AD_SHOWN = "AdPreloaderAdShown"

    /** Swift confirma que el usuario cerró el intersticial. */
    const val AD_DISMISSED = "AdPreloaderAdDismissed"

    /** Swift confirma que no pudo mostrar el intersticial. `userInfo["error"]`. */
    const val SHOW_FAILED = "AdPreloaderShowFailed"

    /** Respuesta de Swift a [IS_READY_REQUESTED]. `userInfo["isReady"]`. */
    const val IS_READY_RESPONSE = "AdPreloaderIsReadyResponse"

    // --- Banner: petición Kotlin -> Swift ---

    /** Pide a Swift que cree un banner. `userInfo["adUnitId"/"bannerId"/"containerView"]`. */
    const val BANNER_LOAD_REQUESTED = "AdMobLoadBannerRequested"

    // --- Banner: respuesta Swift -> Kotlin ---

    /** Swift confirma que el banner cargó. `userInfo["bannerId"]`. */
    const val BANNER_LOADED = "AdMobBannerLoaded"

    /** Swift confirma que el banner falló al cargar. `userInfo["bannerId"/"error"]`. */
    const val BANNER_LOAD_FAILED = "AdMobBannerLoadFailed"
}
