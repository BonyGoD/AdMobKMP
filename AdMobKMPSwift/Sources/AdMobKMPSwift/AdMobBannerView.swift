//
//  AdMobBannerView.swift
//  AdMobKMPSwift
//

import Foundation
import GoogleMobileAds
import UIKit

/// `UIView` que envuelve una `BannerView` del SDK de Google Mobile Ads, para
/// que Compose pueda incrustarla con `UIKitView`.
@objc public class AdMobBannerView: UIView {

    private var bannerView: BannerView?
    private let adUnitId: String
    private let onAdLoaded: () -> Void
    private let onAdFailed: (String) -> Void

    @objc public init(
        adUnitId: String,
        onAdLoaded: @escaping () -> Void,
        onAdFailed: @escaping (String) -> Void
    ) {
        self.adUnitId = adUnitId
        self.onAdLoaded = onAdLoaded
        self.onAdFailed = onAdFailed
        super.init(frame: .zero)
        setupBanner()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setupBanner() {
        let bannerView = BannerView(adSize: AdSizeBanner)
        bannerView.adUnitID = adUnitId

        // La ventana con foco de la escena activa. `UIApplication.shared.windows`
        // está obsoleto desde iOS 15 y no distingue escenas.
        bannerView.rootViewController = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?.rootViewController

        bannerView.delegate = self
        self.bannerView = bannerView

        bannerView.frame = CGRect(x: 0, y: 0, width: 320, height: 50)
        addSubview(bannerView)

        bannerView.load(Request())
    }

    public override func layoutSubviews() {
        super.layoutSubviews()
        // El banner sigue al contenedor: Compose puede medirlo después de que
        // se haya creado, y sin esto se quedaría con el frame inicial.
        bannerView?.frame = bounds
    }

    /// Tamaño intrínseco, para que Compose/UIKit sepan cuánto espacio pedir.
    public override var intrinsicContentSize: CGSize {
        return CGSize(width: 320, height: 50)
    }
}

// MARK: - BannerViewDelegate
extension AdMobBannerView: BannerViewDelegate {

    public func bannerViewDidReceive(_ bannerView: BannerView) {
        onAdLoaded()
    }

    public func bannerView(_ bannerView: BannerView, didFailToReceiveAdWith error: Error) {
        onAdFailed(error.localizedDescription)
    }
}
