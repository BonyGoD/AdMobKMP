package dev.bonygod.admob.kmp.config

/**
 * IDs de prueba oficiales de AdMob, documentados públicamente por Google en
 * https://developers.google.com/admob/android/test-ads y su equivalente iOS.
 * Son los mismos para cualquier desarrollador y cualquier app: no identifican
 * a ningún anunciante ni a ninguna cuenta de AdMob, así que es legítimo
 * tenerlos embebidos en la librería (a diferencia de un ad unit ID real, que
 * nunca puede vivir aquí — ver [dev.bonygod.admob.kmp.config.AdMobConfig]).
 *
 * `internal`: son un detalle de implementación de la resolución de IDs
 * ([getBannerAdUnitId]/[getInterstitialAdUnitId]); quien consume la librería
 * no necesita referenciarlos directamente, le basta con `useTestAds = true`.
 */
internal object AdMobTestIds {
    const val BANNER_AD_UNIT_ID_ANDROID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_UNIT_ID_ANDROID = "ca-app-pub-3940256099942544/1033173712"

    const val BANNER_AD_UNIT_ID_IOS = "ca-app-pub-3940256099942544/2934735716"
    const val INTERSTITIAL_AD_UNIT_ID_IOS = "ca-app-pub-3940256099942544/4411468910"
}
