package dev.bonygod.admob.kmp.ui

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import dev.bonygod.admob.kmp.AdMobKMP
import dev.bonygod.admob.kmp.AdNotifications
import dev.bonygod.admob.kmp.config.getBannerAdUnitId
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotification
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIView
import platform.UIKit.UIScreen

// Nota: AdMobBannerView debe ser importado desde el framework Swift
// import AdMobKMPSwift.AdMobBannerView (esto se hace automáticamente)

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun BannerAd(
    modifier: Modifier,
    adUnitId: String?,
    onAdLoaded: () -> Unit,
    onAdFailedToLoad: (String) -> Unit
) {
    val resolvedAdUnitId = adUnitId ?: AdMobKMP.getBannerAdUnitId()
    // Se fuerza una altura de 50.dp para que Compose asigne el espacio correcto.
    // Un UIView plano no tiene intrinsicContentSize, por lo que sin esta restricción
    // Compose le asignaría altura = 0 y el banner quedaría invisible.
    UIKitView(
        modifier = modifier.height(50.dp),
        factory = {
            // Crear el AdMobBannerView directamente
            // Swift lo expone como una clase accesible desde Kotlin
            createAdMobBannerView(resolvedAdUnitId, onAdLoaded, onAdFailedToLoad)
        }
    )
}

// Función que crea el banner usando la clase Swift
@OptIn(ExperimentalForeignApi::class)
private fun createAdMobBannerView(
    adUnitId: String,
    onAdLoaded: () -> Unit,
    onAdFailedToLoad: (String) -> Unit
): UIView {
    // Enviar notificación para crear el banner de forma síncrona
    val bannerId = "banner_${adUnitId.hashCode()}"
    val containerView = UIView()
    containerView.setTag(bannerId.hashCode().toLong())

    // IMPORTANTE: Configurar un frame inicial para que el banner sea visible
    // El tamaño estándar de un banner de AdMob es 320x50
    val screenWidth = UIScreen.mainScreen.bounds.useContents { this.size.width }
    containerView.setFrame(CGRectMake(0.0, 0.0, screenWidth, 50.0))

    // Configurar observers antes de enviar la solicitud
    var loadedObserver: Any? = null
    var failedObserver: Any? = null


    loadedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
        name = AdNotifications.BANNER_LOADED,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
        usingBlock = { notification: NSNotification? ->
            val id = notification?.userInfo?.get("bannerId") as? String
            if (id == bannerId) {
                onAdLoaded()
                loadedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                failedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
            }
        }
    )

    failedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
        name = AdNotifications.BANNER_LOAD_FAILED,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
        usingBlock = { notification: NSNotification? ->
            val id = notification?.userInfo?.get("bannerId") as? String
            if (id == bannerId) {
                val error = notification?.userInfo?.get("error") as? String ?: "Unknown error"
                onAdFailedToLoad(error)
                loadedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                failedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
            }
        }
    )

    // Enviar solicitud para crear el banner
    val userInfo: Map<Any?, *> = mapOf(
        "adUnitId" to adUnitId,
        "bannerId" to bannerId,
        "containerView" to containerView
    )

    NSNotificationCenter.defaultCenter.postNotificationName(
        AdNotifications.BANNER_LOAD_REQUESTED,
        `object` = null,
        userInfo = userInfo
    )


    return containerView
}
