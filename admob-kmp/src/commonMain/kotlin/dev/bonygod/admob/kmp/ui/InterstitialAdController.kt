package dev.bonygod.admob.kmp.ui

import androidx.compose.runtime.Composable

/**
 * Intersticial bajo demanda: para mostrarlo en un momento arbitrario de la
 * app (no como pantalla completa, ver [InterstitialAdScreen] para eso).
 *
 * Con `AdMobConfig.interstitialEnabled = false`, las tres operaciones son
 * no-ops seguros: [preload] no hace nada, [isReady] siempre devuelve `false`
 * y [show] llama a `onDismissed` de inmediato. Ninguna vía hace peticiones
 * de red con el intersticial desactivado.
 */
interface InterstitialAdController {
    /** Inicia la precarga. No bloquea; el resultado se refleja en [isReady]. */
    fun preload()

    /** `true` si hay un intersticial precargado listo para [show]. */
    fun isReady(): Boolean

    /**
     * Muestra el intersticial precargado. Si no hay ninguno listo, o el
     * intersticial está desactivado, llama a [onDismissed] de inmediato en
     * vez de fallar o quedarse esperando.
     */
    fun show(onDismissed: () -> Unit = {})
}

/**
 * Recuerda un [InterstitialAdController] ligado a la composición actual.
 */
@Composable
expect fun rememberInterstitialAd(): InterstitialAdController
