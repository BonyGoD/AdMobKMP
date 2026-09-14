package dev.bonygod.admob.kmp.config

import dev.bonygod.admob.kmp.AdMobKMP

/**
 * Resuelven el ad unit ID activo para cada plataforma a partir de
 * `AdMobKMP.config()`. Públicas (no `internal`): tanto el consumidor Kotlin
 * como el puente Swift (vía `InterstitialAdPreloader`) las necesitan.
 *
 * La lógica es la misma en las dos plataformas (ver los `actual`):
 * 1. Si `useTestAds` está activo, se sirve siempre el ID de prueba de Google.
 * 2. Si no, se sirve el ID de producción de [AdMobConfig].
 * 3. Si ese ID de producción está en blanco (la app no lo configuró todavía),
 *    se cae al ID de prueba en vez de pasarle una cadena vacía al SDK de
 *    AdMob, que fallaría con un error críptico y tarde, al hacer la petición
 *    de red. Se avisa por consola para que el hueco no pase desapercibido.
 */
expect fun AdMobKMP.getBannerAdUnitId(): String
expect fun AdMobKMP.getInterstitialAdUnitId(): String
