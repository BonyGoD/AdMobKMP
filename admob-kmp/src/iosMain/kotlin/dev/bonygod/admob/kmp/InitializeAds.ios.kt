package dev.bonygod.admob.kmp

/**
 * Punto de arranque para iOS.
 *
 * A diferencia de Android, el SDK de Google Mobile Ads en iOS lo inicializa
 * el propio código Swift de la app consumidora (llamando a
 * `MobileAds.shared.start(completionHandler:)`, típicamente en el `init` de
 * la `App` o en el `AppDelegate`) — Kotlin/Native no puede enlazar ni arrancar
 * ese SDK desde aquí. Esta función solo dispara, si el intersticial está
 * activado, la precarga a través del puente de notificaciones que gestiona
 * [InterstitialAdPreloader].
 *
 * No es `expect`/`actual`: en Android hace falta un `Context` y aquí no, así
 * que cada plataforma declara su propia función (ver la de `androidMain`).
 *
 * Llamar una única vez, lo antes posible en el arranque de la app iOS,
 * después de `AdMobKMP.configure(...)` y después de que Swift haya
 * inicializado el SDK.
 */
fun AdMobKMP.initializeAds() {
    if (isInterstitialEnabled()) {
        InterstitialAdPreloader.preloadInterstitial()
    }
}
