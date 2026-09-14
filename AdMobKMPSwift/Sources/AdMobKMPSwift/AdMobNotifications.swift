//
//  AdMobNotifications.swift
//  AdMobKMPSwift
//

import Foundation

/// Nombres de `NSNotification` que forman el puente entre el módulo Kotlin
/// `admob-kmp` y este paquete Swift.
///
/// **Contrato compartido, no automático.** Cada nombre existe por duplicado:
/// aquí y en `AdNotifications.kt` (en `admob-kmp/src/iosMain`). No hay ningún
/// tipo común que los sincronice, así que cambiar un valor en un lado y no en
/// el otro los desincroniza en silencio: la notificación deja de emparejarse y
/// el componente se queda esperando para siempre, sin error visible.
///
/// Si tocas algo de esta tabla, toca `AdNotifications.kt` en el mismo commit.
public enum AdMobNotifications {

    // MARK: - Intersticial: petición Kotlin -> Swift

    /// Kotlin pide precargar un intersticial. `userInfo["adUnitId"]`.
    public static let preloadRequested = Notification.Name("AdPreloaderPreloadRequested")

    /// Kotlin pide mostrar el intersticial ya precargado, si lo hay.
    public static let showRequested = Notification.Name("AdPreloaderShowRequested")

    /// Kotlin pregunta si hay un intersticial listo.
    public static let isReadyRequested = Notification.Name("AdPreloaderIsReadyRequested")

    // MARK: - Intersticial: respuesta Swift -> Kotlin

    /// La precarga terminó con éxito. La espera `rememberInterstitialAd()`.
    public static let preloadCompleted = Notification.Name("AdPreloaderPreloadCompleted")

    /// La precarga falló. `userInfo["error"]`.
    public static let preloadFailed = Notification.Name("AdPreloaderPreloadFailed")

    /// El intersticial se mostró en pantalla.
    public static let adShown = Notification.Name("AdPreloaderAdShown")

    /// El usuario cerró el intersticial.
    public static let adDismissed = Notification.Name("AdPreloaderAdDismissed")

    /// No se pudo mostrar el intersticial. `userInfo["error"]`.
    public static let showFailed = Notification.Name("AdPreloaderShowFailed")

    /// Respuesta a `isReadyRequested`. `userInfo["isReady"]`.
    public static let isReadyResponse = Notification.Name("AdPreloaderIsReadyResponse")

    // MARK: - Banner: petición Kotlin -> Swift

    /// Kotlin pide crear un banner.
    /// `userInfo["adUnitId"]`, `userInfo["bannerId"]`, `userInfo["containerView"]`.
    public static let bannerLoadRequested = Notification.Name("AdMobLoadBannerRequested")

    // MARK: - Banner: respuesta Swift -> Kotlin

    /// El banner cargó. `userInfo["bannerId"]`.
    public static let bannerLoaded = Notification.Name("AdMobBannerLoaded")

    /// El banner falló al cargar. `userInfo["bannerId"]`, `userInfo["error"]`.
    public static let bannerLoadFailed = Notification.Name("AdMobBannerLoadFailed")
}
