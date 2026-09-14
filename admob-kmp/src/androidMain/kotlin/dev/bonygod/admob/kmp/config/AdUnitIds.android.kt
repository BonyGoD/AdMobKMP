package dev.bonygod.admob.kmp.config

import dev.bonygod.admob.kmp.AdMobKMP

actual fun AdMobKMP.getBannerAdUnitId(): String {
    val config = config()
    if (config.useTestAds) return AdMobTestIds.BANNER_AD_UNIT_ID_ANDROID

    val prodId = config.androidBannerId
    if (prodId.isBlank()) {
        // Preferible caer al ID de prueba y avisar por consola que pasarle
        // "" al SDK de AdMob, que falla con un error críptico y tarde (al
        // hacer la petición de red, no al configurar).
        println("⚠️ [AdMobKMP] androidBannerId no configurado en AdMobConfig. Sirviendo banner de prueba de Google.")
        return AdMobTestIds.BANNER_AD_UNIT_ID_ANDROID
    }
    return prodId
}

actual fun AdMobKMP.getInterstitialAdUnitId(): String {
    val config = config()
    if (config.useTestAds) return AdMobTestIds.INTERSTITIAL_AD_UNIT_ID_ANDROID

    val prodId = config.androidInterstitialId
    if (prodId.isBlank()) {
        println("⚠️ [AdMobKMP] androidInterstitialId no configurado en AdMobConfig. Sirviendo intersticial de prueba de Google.")
        return AdMobTestIds.INTERSTITIAL_AD_UNIT_ID_ANDROID
    }
    return prodId
}
