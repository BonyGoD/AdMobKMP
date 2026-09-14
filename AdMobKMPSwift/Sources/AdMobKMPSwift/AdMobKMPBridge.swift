//
//  AdMobKMPBridge.swift
//  AdMobKMPSwift
//

import Foundation
import GoogleMobileAds

/// Único punto de entrada del lado Swift de AdMobKMP.
///
/// Inicializa el SDK de Google Mobile Ads (que Kotlin/Native no puede arrancar
/// por sí mismo) y da de alta los dos puentes que atienden a Kotlin: el de
/// banner (`AdMobCallbackHelper`) y el de intersticial
/// (`AdMobInterstitialBridge`). Los singletons registran sus observers en su
/// `init`, así que basta con tocarlos una vez.
///
/// Uso en la app consumidora, una sola línea en el arranque:
/// ```swift
/// @main
/// struct iOSApp: App {
///     init() {
///         AdMobKMPBridge.start()
///     }
///     ...
/// }
/// ```
///
/// La precarga del intersticial no se dispara aquí: la pide el lado Kotlin con
/// `AdMobKMP.initializeAds()`, que es quien conoce el `AdMobConfig` y el
/// interruptor `interstitialEnabled`.
@objc public class AdMobKMPBridge: NSObject {

    /// Arranca el SDK de anuncios y los puentes con Kotlin. Idempotente en la
    /// práctica: los puentes son singletons y `MobileAds.start` puede llamarse
    /// más de una vez sin efectos, pero lo natural es llamarlo una sola vez.
    @objc public static func start() {
        MobileAds.shared.start(completionHandler: nil)

        _ = AdMobCallbackHelper.shared
        _ = AdMobInterstitialBridge.shared

        print("✅ [AdMobKMP-Swift] Puentes de anuncios inicializados")
    }
}
