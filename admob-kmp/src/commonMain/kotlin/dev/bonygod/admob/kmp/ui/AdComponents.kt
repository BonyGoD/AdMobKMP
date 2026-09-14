package dev.bonygod.admob.kmp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Banner de AdMob. El caso simple: una llamada, sin pasar ningún ID a mano.
 *
 * @param adUnitId ad unit ID a usar. `null` (por defecto) resuelve el de
 * [dev.bonygod.admob.kmp.config.AdMobConfig] dado en `AdMobKMP.configure()`.
 * Pásalo explícito solo si necesitas un banner con un ID distinto al general
 * de la app (p. ej. varias pantallas con inventarios distintos).
 */
@Composable
expect fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String? = null,
    onAdLoaded: () -> Unit = {},
    onAdFailedToLoad: (String) -> Unit = {},
)
