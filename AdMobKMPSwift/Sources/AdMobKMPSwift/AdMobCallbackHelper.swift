//
//  AdMobCallbackHelper.swift
//  AdMobKMPSwift
//

import Foundation
import UIKit
import GoogleMobileAds

/// Atiende las peticiones de **banner** que llegan desde Kotlin.
///
/// Kotlin no puede instanciar una `BannerView` del SDK de Google Mobile Ads
/// (Kotlin/Native no enlaza ese SDK), así que `BannerAd()` crea un `UIView`
/// contenedor vacío, lo manda por `NSNotificationCenter` y este helper le
/// mete dentro el banner real.
///
/// El intersticial no pasa por aquí: lo lleva `AdMobInterstitialBridge`.
@objc public class AdMobCallbackHelper: NSObject {

    @objc public static let shared = AdMobCallbackHelper()

    private override init() {
        super.init()
        setupObservers()
    }

    private func setupObservers() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleLoadBannerRequest),
            name: AdMobNotifications.bannerLoadRequested,
            object: nil
        )
    }

    @objc private func handleLoadBannerRequest(_ notification: Notification) {
        guard let userInfo = notification.userInfo,
              let adUnitId = userInfo["adUnitId"] as? String,
              let bannerId = userInfo["bannerId"] as? String,
              let containerView = userInfo["containerView"] as? UIView else {
            print("❌ [AdMobKMP-Swift] Faltan parámetros en la petición de banner")
            return
        }

        print("🔵 [AdMobKMP-Swift] Petición de banner: \(adUnitId) (\(bannerId))")

        DispatchQueue.main.async {
            let bannerView = AdMobBannerView(
                adUnitId: adUnitId,
                onAdLoaded: {
                    print("✅ [AdMobKMP-Swift] Banner cargado")
                    NotificationCenter.default.post(
                        name: AdMobNotifications.bannerLoaded,
                        object: nil,
                        userInfo: ["bannerId": bannerId]
                    )
                },
                onAdFailed: { error in
                    print("❌ [AdMobKMP-Swift] Banner falló: \(error)")
                    NotificationCenter.default.post(
                        name: AdMobNotifications.bannerLoadFailed,
                        object: nil,
                        userInfo: ["bannerId": bannerId, "error": error]
                    )
                }
            )

            // El frame se fija DESPUÉS de crear el banner. Si Compose todavía no
            // ha medido el containerView, su ancho es 0, así que se cae al ancho
            // de pantalla; la altura es la estándar de un banner de AdMob.
            let width = containerView.bounds.width > 0
                ? containerView.bounds.width
                : UIScreen.main.bounds.width
            bannerView.frame = CGRect(x: 0, y: 0, width: width, height: 50)
            bannerView.autoresizingMask = [.flexibleWidth, .flexibleHeight]

            containerView.addSubview(bannerView)
        }
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}
