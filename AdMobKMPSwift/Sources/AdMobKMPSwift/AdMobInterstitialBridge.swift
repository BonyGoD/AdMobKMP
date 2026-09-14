//
//  AdMobInterstitialBridge.swift
//  AdMobKMPSwift
//
//  Fusión de los dos puentes de intersticial que convivían en ListaCompra:
//  se conserva la lógica de `AdPreloader.swift` (la que estaba en producción,
//  con `connectedScenes` para localizar el root view controller y el delegate
//  en la propia instancia) y se descarta la del antiguo
//  `AdMobInterstitialBridge` estático, que nunca llegó a usarse y resolvía el
//  root view controller con `UIApplication.shared.windows`, hoy obsoleto.
//

import Foundation
import GoogleMobileAds
import UIKit

/// Precarga y muestra anuncios intersticiales, atendiendo las peticiones que
/// llegan desde Kotlin por `NSNotificationCenter`.
///
/// El estado del anuncio vive aquí, no en Kotlin: el lado Kotlin solo pide
/// ("precarga", "muestra") y escucha el resultado. Ver `AdMobNotifications`
/// para la tabla completa de nombres.
@objc public class AdMobInterstitialBridge: NSObject {

    @objc public static let shared = AdMobInterstitialBridge()

    private var interstitialAd: InterstitialAd?
    private var isLoading = false

    private override init() {
        super.init()
        setupNotificationObservers()
    }

    // MARK: - Observers

    private func setupNotificationObservers() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handlePreloadRequest),
            name: AdMobNotifications.preloadRequested,
            object: nil
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleShowRequest),
            name: AdMobNotifications.showRequested,
            object: nil
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleIsReadyRequest),
            name: AdMobNotifications.isReadyRequested,
            object: nil
        )
    }

    @objc private func handlePreloadRequest(_ notification: Notification) {
        guard let adUnitId = notification.userInfo?["adUnitId"] as? String else {
            print("❌ [AdMobInterstitialBridge] Falta adUnitId en la petición de precarga")
            return
        }
        preloadAd(adUnitId: adUnitId)
    }

    @objc private func handleShowRequest(_ notification: Notification) {
        print("🔵 [AdMobInterstitialBridge] Petición de mostrar recibida")

        guard let rootVC = Self.rootViewController() else {
            print("❌ [AdMobInterstitialBridge] No se encontró root view controller")
            NotificationCenter.default.post(
                name: AdMobNotifications.showFailed,
                object: nil,
                userInfo: ["error": "No root view controller"]
            )
            return
        }

        showAd(
            from: rootVC,
            onAdShown: {
                NotificationCenter.default.post(name: AdMobNotifications.adShown, object: nil)
            },
            onAdDismissed: {
                NotificationCenter.default.post(name: AdMobNotifications.adDismissed, object: nil)
            },
            onAdFailedToShow: { error in
                NotificationCenter.default.post(
                    name: AdMobNotifications.showFailed,
                    object: nil,
                    userInfo: ["error": error]
                )
            }
        )
    }

    @objc private func handleIsReadyRequest(_ notification: Notification) {
        NotificationCenter.default.post(
            name: AdMobNotifications.isReadyResponse,
            object: nil,
            userInfo: ["isReady": isAdReady()]
        )
    }

    // MARK: - API pública

    /// Precarga un intersticial. Al terminar publica `preloadCompleted` o
    /// `preloadFailed`, que es lo que permite a `rememberInterstitialAd()` en
    /// Kotlin saber si hay anuncio listo sin sondear.
    @objc public func preloadAd(adUnitId: String) {
        if isLoading || interstitialAd != nil {
            print("🟡 [AdMobInterstitialBridge] Ya hay un anuncio cargado o cargándose")
            return
        }

        isLoading = true
        print("🟡 [AdMobInterstitialBridge] Precargando intersticial: \(adUnitId)")

        InterstitialAd.load(
            with: adUnitId,
            request: Request()
        ) { [weak self] ad, error in
            guard let self = self else { return }

            self.isLoading = false

            if let error = error {
                print("❌ [AdMobInterstitialBridge] Fallo al precargar: \(error.localizedDescription)")
                NotificationCenter.default.post(
                    name: AdMobNotifications.preloadFailed,
                    object: nil,
                    userInfo: ["error": error.localizedDescription]
                )
                return
            }

            print("✅ [AdMobInterstitialBridge] Intersticial precargado")
            self.interstitialAd = ad
            NotificationCenter.default.post(name: AdMobNotifications.preloadCompleted, object: nil)
        }
    }

    @objc public func showAd(
        from viewController: UIViewController,
        onAdShown: @escaping () -> Void,
        onAdDismissed: @escaping () -> Void,
        onAdFailedToShow: @escaping (String) -> Void
    ) {
        guard let interstitial = interstitialAd else {
            print("❌ [AdMobInterstitialBridge] No hay anuncio cargado que mostrar")
            onAdFailedToShow("No ad loaded")
            return
        }

        print("🟢 [AdMobInterstitialBridge] Mostrando intersticial precargado")

        interstitial.fullScreenContentDelegate = self

        onAdShownCallback = onAdShown
        onAdDismissedCallback = onAdDismissed
        onAdFailedToShowCallback = onAdFailedToShow

        interstitial.present(from: viewController)
    }

    @objc public func isAdReady() -> Bool {
        return interstitialAd != nil
    }

    // MARK: - Interno

    /// Root view controller de la ventana activa. `UIApplication.shared.windows`
    /// está obsoleto desde iOS 15 y no distingue escenas, así que se recorren
    /// las `connectedScenes` para dar con la ventana que tiene el foco.
    private static func rootViewController() -> UIViewController? {
        return UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?.rootViewController
    }

    private var onAdShownCallback: (() -> Void)?
    private var onAdDismissedCallback: (() -> Void)?
    private var onAdFailedToShowCallback: ((String) -> Void)?

    private func clearCallbacks() {
        onAdShownCallback = nil
        onAdDismissedCallback = nil
        onAdFailedToShowCallback = nil
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}

// MARK: - FullScreenContentDelegate
extension AdMobInterstitialBridge: FullScreenContentDelegate {

    public func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
        print("✅ [AdMobInterstitialBridge] El anuncio va a presentarse")
        onAdShownCallback?()
    }

    public func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        print("👋 [AdMobInterstitialBridge] Anuncio cerrado")
        interstitialAd = nil
        onAdDismissedCallback?()
        clearCallbacks()
    }

    public func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        print("❌ [AdMobInterstitialBridge] Fallo al mostrar: \(error.localizedDescription)")
        interstitialAd = nil
        onAdFailedToShowCallback?(error.localizedDescription)
        clearCallbacks()
    }
}
