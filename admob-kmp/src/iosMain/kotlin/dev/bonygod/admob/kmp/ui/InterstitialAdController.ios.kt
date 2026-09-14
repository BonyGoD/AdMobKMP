package dev.bonygod.admob.kmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bonygod.admob.kmp.AdMobKMP
import dev.bonygod.admob.kmp.AdNotifications
import dev.bonygod.admob.kmp.InterstitialAdPreloader
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

/**
 * Implementación iOS de [InterstitialAdController].
 *
 * El estado real del anuncio (cargado o no) vive en Swift
 * (`AdPreloader.shared`), así que no hay nada que consultar de forma
 * síncrona desde Kotlin. [ready] es una copia local que se mantiene al día
 * escuchando las notificaciones que Swift emite ([AdNotifications.PRELOAD_COMPLETED],
 * [AdNotifications.PRELOAD_FAILED], [AdNotifications.AD_DISMISSED],
 * [AdNotifications.SHOW_FAILED]) — no se asume "tras N intentos ya estará
 * listo", que es el apaño que sí sigue usando `ShowPreloadedInterstitial` por
 * compatibilidad con el código ya probado en producción.
 *
 * Los observers se registran/liberan desde [rememberInterstitialAd] con un
 * `DisposableEffect`, atados al ciclo de vida de la composición que pidió el
 * controller.
 */
@OptIn(ExperimentalForeignApi::class)
private class IosInterstitialAdController : InterstitialAdController {

    var ready by mutableStateOf(false)
        private set

    private var preloadCompletedObserver: Any? = null
    private var preloadFailedObserver: Any? = null
    private var dismissedObserver: Any? = null
    private var showFailedObserver: Any? = null

    fun registerObservers() {
        preloadCompletedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AdNotifications.PRELOAD_COMPLETED,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _: NSNotification? -> ready = true }
        )
        preloadFailedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AdNotifications.PRELOAD_FAILED,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _: NSNotification? -> ready = false }
        )
        dismissedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AdNotifications.AD_DISMISSED,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _: NSNotification? -> ready = false }
        )
        showFailedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AdNotifications.SHOW_FAILED,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _: NSNotification? -> ready = false }
        )
    }

    fun unregisterObservers() {
        preloadCompletedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        preloadFailedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        dismissedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        showFailedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
    }

    override fun preload() {
        if (!AdMobKMP.isInterstitialEnabled()) return
        InterstitialAdPreloader.preloadInterstitial()
    }

    override fun isReady(): Boolean {
        if (!AdMobKMP.isInterstitialEnabled()) return false
        return ready
    }

    override fun show(onDismissed: () -> Unit) {
        if (!AdMobKMP.isInterstitialEnabled() || !ready) {
            onDismissed()
            return
        }

        var localDismissedObserver: Any? = null
        var localShowFailedObserver: Any? = null

        localDismissedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AdNotifications.AD_DISMISSED,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _: NSNotification? ->
                onDismissed()
                localDismissedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                localShowFailedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
            }
        )

        localShowFailedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AdNotifications.SHOW_FAILED,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _: NSNotification? ->
                onDismissed()
                localDismissedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                localShowFailedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
            }
        )

        NSNotificationCenter.defaultCenter.postNotificationName(
            AdNotifications.SHOW_REQUESTED,
            `object` = null
        )
    }
}

@Composable
actual fun rememberInterstitialAd(): InterstitialAdController {
    val controller = remember { IosInterstitialAdController() }
    DisposableEffect(Unit) {
        controller.registerObservers()
        onDispose { controller.unregisterObservers() }
    }
    return controller
}
