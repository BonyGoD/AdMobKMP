package dev.bonygod.admob.kmp.ui

import androidx.compose.runtime.Composable

/**
 * Motor interno de [InterstitialAdScreen]: muestra el intersticial que ya se
 * dejó precargado (en Android por [dev.bonygod.admob.kmp.InterstitialAdManager],
 * en iOS por el puente de notificaciones con Swift). No es API pública: quien
 * consume la librería usa [InterstitialAdScreen] o [rememberInterstitialAd].
 */
@Composable
internal expect fun ShowPreloadedInterstitial(
    onAdShown: () -> Unit,
    onAdDismissed: () -> Unit,
    onAdFailedToShow: () -> Unit
)
