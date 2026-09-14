package dev.bonygod.admob.kmp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.bonygod.admob.kmp.AdMobKMP

/**
 * Pantalla de intersticial completa. Sustituye al `AdLoadingScreen` que vivía
 * en la app: no conoce ningún `Navigator`, `Routes` ni indicador de carga
 * propio de ninguna app — quien consume decide la navegación en [onFinished]
 * y puede sustituir el indicador con [loading].
 *
 * Pinta [loading] durante todo el proceso y llama a [onFinished] exactamente
 * una vez:
 * - Si `AdMobKMP.isInterstitialEnabled()` es `false`, de inmediato, sin
 *   precargar ni pedir nada (cero peticiones de red).
 * - Si está activo, cuando el intersticial se cierra o cuando falla al
 *   mostrarse. Que el anuncio se haya *mostrado* no dispara [onFinished]:
 *   solo cerrarse o fallar cuentan como "terminado".
 */
@Composable
fun InterstitialAdScreen(
    onFinished: () -> Unit,
    loading: @Composable () -> Unit = { DefaultAdLoading() },
) {
    // Blindaje: onFinished puede intentar dispararse más de una vez (p. ej.
    // en iOS, donde "dismissed" y "failed" llegan como notificaciones
    // independientes). Una doble navegación en la app consumidora es un bug
    // feo de diagnosticar, así que aquí se corta de raíz.
    var finished by remember { mutableStateOf(false) }
    fun finishOnce() {
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    loading()

    if (AdMobKMP.isInterstitialEnabled()) {
        ShowPreloadedInterstitial(
            onAdShown = {},
            onAdDismissed = { finishOnce() },
            onAdFailedToShow = { finishOnce() },
        )
    } else {
        LaunchedEffect(Unit) {
            finishOnce()
        }
    }
}

/**
 * Indicador de carga por defecto de [InterstitialAdScreen]. Público para que
 * el consumidor pueda reutilizarlo; lo normal es sustituirlo por el propio
 * de la app a través del parámetro `loading`.
 */
@Composable
fun DefaultAdLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
