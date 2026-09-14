package dev.bonygod.admob.kmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.bonygod.admob.kmp.AdNotifications
import dev.bonygod.admob.kmp.InterstitialAdPreloader
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun ShowPreloadedInterstitial(
    onAdShown: () -> Unit,
    onAdDismissed: () -> Unit,
    onAdFailedToShow: () -> Unit
) {
    var hasAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (hasAttempted) return@LaunchedEffect
        hasAttempted = true

        // Configurar observers para las respuestas
        var shownObserver: Any? = null
        var dismissedObserver: Any? = null
        var failedObserver: Any? = null

        shownObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AdNotifications.AD_SHOWN,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _: NSNotification? ->
                println("✅ [iOS] Ad shown")
                onAdShown()
            }
        )

        dismissedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AdNotifications.AD_DISMISSED,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _: NSNotification? ->
                println("👋 [iOS] Ad dismissed")
                onAdDismissed()

                // Limpiar observers
                shownObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                dismissedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                failedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }

                // Precargar el siguiente anuncio. Se delega en el preloader en
                // vez de publicar la notificación a mano: así la precarga pasa
                // por la guarda de `interstitialEnabled` en un solo sitio.
                InterstitialAdPreloader.preloadInterstitial()
            }
        )

        failedObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AdNotifications.SHOW_FAILED,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { notification: NSNotification? ->
                val error = notification?.userInfo?.get("error") as? String ?: "Unknown error"
                println("❌ [iOS] Failed to show: $error")
                onAdFailedToShow()

                // Limpiar observers
                shownObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                dismissedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
                failedObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }

                // Intentar precargar de nuevo, por la misma vía guardada.
                InterstitialAdPreloader.preloadInterstitial()
            }
        )

        // Esperar un poco para verificar si el anuncio está listo
        // Si no está listo, esperamos hasta 3 segundos
        var attempts = 0
        var isReady = false

        while (!isReady && attempts < 10) {
            // Verificar si el anuncio está listo
            NSNotificationCenter.defaultCenter.postNotificationName(
                AdNotifications.IS_READY_REQUESTED,
                `object` = null
            )

            // Pequeña espera para la respuesta
            delay(300)
            attempts++

            // Por simplicidad, asumimos que después de algunos intentos está listo
            // o lo intentamos mostrar de todas formas
            if (attempts >= 3) {
                isReady = true
            }
        }

        // Enviar solicitud para mostrar el anuncio
        println("🔵 [iOS] Requesting to show ad")
        NSNotificationCenter.defaultCenter.postNotificationName(
            AdNotifications.SHOW_REQUESTED,
            `object` = null
        )
    }
}
