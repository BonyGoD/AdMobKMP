package dev.bonygod.admob.kmp.config

import dev.bonygod.admob.kmp.AdMobKMP

actual fun AdMobKMP.getBannerAdUnitId(): String {
    val config = config()
    if (config.useTestAds) return AdMobTestIds.BANNER_AD_UNIT_ID_IOS

    val prodId = config.iosBannerId
    if (prodId.isBlank()) {
        // Preferible caer al ID de prueba y avisar por consola que pasarle
        // "" al SDK de AdMob, que falla con un error críptico y tarde (al
        // hacer la petición de red, no al configurar).
        println("⚠️ [AdMobKMP] iosBannerId no configurado en AdMobConfig. Sirviendo banner de prueba de Google.")
        return AdMobTestIds.BANNER_AD_UNIT_ID_IOS
    }
    return prodId
}

actual fun AdMobKMP.getInterstitialAdUnitId(): String {
    val config = config()
    if (config.useTestAds) return AdMobTestIds.INTERSTITIAL_AD_UNIT_ID_IOS

    val prodId = config.iosInterstitialId
    if (prodId.isBlank()) {
        println("⚠️ [AdMobKMP] iosInterstitialId no configurado en AdMobConfig. Sirviendo intersticial de prueba de Google.")
        return AdMobTestIds.INTERSTITIAL_AD_UNIT_ID_IOS
    }
    return prodId
}
