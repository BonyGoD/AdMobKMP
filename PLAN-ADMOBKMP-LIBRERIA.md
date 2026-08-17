# Plan — AdMobKMP, librería de anuncios para KMP

**Repo nuevo:** `BonyGoD/AdMobKMP`
**Coordenada Gradle:** `com.github.BonyGoD.AdMobKMP:admob-kmp`
**Producto Swift:** `AdMobKMPSwift`
**Estado:** sin empezar. Va después de `feature/onboarding-sin-registro`.

---

## 1. Objetivo

Que integrar anuncios en cualquier proyecto KMP sea esto y nada más:

```kotlin
// Banner
BannerAd(modifier = Modifier.fillMaxWidth())

// Intersticial como pantalla completa
InterstitialAdScreen(onFinished = { navigator.clearAndNavigateTo(Routes.Home) })
```

Sin pasar ad unit IDs en cada llamada, sin montar el puente de iOS a mano, sin
duplicar `expect/actual`. La configuración se da una vez al arrancar la app y los
componentes la leen de ahí.

Se replica el patrón ya probado dos veces: `SignInKMP` (2.0.1) y
`CrashlyticsKMP` (1.0.0), ambos consumidos hoy por ListaCompra.

## 2. El hallazgo que hace esto viable

**JitPack sí publica los klibs de iOS**, aunque compile en Linux. Verificado en
la caché de Gradle contra `SignInKMP:2.0.1`:

```
signin-kmp-android-2.0.1.aar
signin-kmp-iosarm64-2.0.1.klib
signin-kmp-iossimulatorarm64-2.0.1.klib
signin-kmp-iosx64-2.0.1.klib
signin-kmp-2.0.1.jar            (metadata common)
```

Kotlin cross-compila klibs para targets Apple desde un host no-Mac. Lo que **no**
puede hacer en Linux es enlazar el `.framework`, y por eso la mitad Swift viaja
por SPM desde el mismo repo de GitHub, no por JitPack. Es exactamente la
arquitectura de `SignInKMP`, y es la que se copia.

## 3. Qué se mueve al repo nuevo

Inventario exacto de lo que hoy vive disperso en ListaCompra:

| Origen actual | Destino |
|---|---|
| `composeApp/src/commonMain/.../ads/AdConstants.kt` | `admob-kmp/src/commonMain/.../config/` (reescrito, ver §4) |
| `composeApp/src/commonMain/.../ads/ui/AdComponents.kt` | `admob-kmp/src/commonMain/.../ui/` |
| `composeApp/src/commonMain/.../ads/README.md` | `README.md` del repo |
| `composeApp/src/androidMain/.../ads/AdConstants.android.kt` | `admob-kmp/src/androidMain/` |
| `composeApp/src/androidMain/.../ads/InterstitialAdManager.kt` | `admob-kmp/src/androidMain/` |
| `composeApp/src/androidMain/.../ads/ui/AdComponents.android.kt` | `admob-kmp/src/androidMain/` |
| `composeApp/src/iosMain/.../ads/AdConstants.ios.kt` | `admob-kmp/src/iosMain/` |
| `composeApp/src/iosMain/.../ads/InterstitialAdPreloader.kt` | `admob-kmp/src/iosMain/` |
| `composeApp/src/iosMain/.../ads/ui/AdComponents.ios.kt` | `admob-kmp/src/iosMain/` |
| `composeApp/src/commonMain/.../login/ui/screens/ShowPreloadedInterstitial.kt` | `admob-kmp/src/commonMain/.../ui/` |
| `…/ShowPreloadedInterstitial.android.kt` | `admob-kmp/src/androidMain/.../ui/` |
| `…/ShowPreloadedInterstitial.ios.kt` | `admob-kmp/src/iosMain/.../ui/` |
| `composeApp/src/commonMain/.../login/ui/screens/AdLoadingScreen.kt` | Base de `InterstitialAdScreen` (ver §5) |
| `AdMobKMPSwift/` (paquete local) | Raíz del repo nuevo |
| `iosApp/iosApp/AdPreloader.swift` | **Dentro** de `AdMobKMPSwift/Sources/` |

Nótese que `ShowPreloadedInterstitial*` y `AdLoadingScreen` viven hoy en
`login/ui/screens/`, que no es su sitio: son código de anuncios colocado donde se
usaba. La mudanza los recoloca.

## 4. El cambio de fondo: la configuración

Es el único punto donde no vale copiar y pegar. Hoy `AdConstants.kt` hace:

```kotlin
import dev.bonygod.listacompra.BuildConfig
private val PROD_BANNER_AD_UNIT_ID_ANDROID = BuildConfig.ADMOB_ANDROID_BANNER
```

Eso lee el `BuildConfig` **de la app**, generado por
`composeApp/build.gradle.kts:144-159` desde `local.properties`. Una librería no
puede depender del `BuildConfig` de quien la consume, y hornear IDs propios en el
AAR sería peor: cada app tiene sus ad units y publicar los de ListaCompra dentro
de la librería sería un error de seguridad y de política de AdMob.

> Cuidado con copiar aquí a `SignInKMP`: ese módulo **sí** usa `buildConfig` con
> su propio `local.properties` (`FIREBASE_API_KEY`, `CLIENT_ID`), lo que hornea
> los valores del autor en el artefacto publicado. No repetir ese patrón en
> AdMobKMP.

**Diseño:** configuración explícita e inyectada una sola vez.

```kotlin
// commonMain — config/AdMobConfig.kt
data class AdMobConfig(
    val androidBannerId: String = "",
    val androidInterstitialId: String = "",
    val iosBannerId: String = "",
    val iosInterstitialId: String = "",
    val useTestAds: Boolean = false,
    val interstitialEnabled: Boolean = true,
)

object AdMobKMP {
    fun configure(config: AdMobConfig)
    fun config(): AdMobConfig       // usado por los componentes
    fun isInterstitialEnabled(): Boolean
}
```

- Los **IDs de prueba de Google** se quedan dentro de la librería: son públicos y
  permiten arrancar con `useTestAds = true` sin configurar nada.
- `interstitialEnabled` es el mismo interruptor que ya se aplicó en ListaCompra
  el 17 ago 2026 (`AdConstants.INTERSTITIAL_ENABLED`), promovido a configuración.
  Con él en `false` no se precarga ni se muestra, y `InterstitialAdScreen` llama
  a `onFinished` de inmediato.
- La resolución test/producción y Android/iOS queda dentro de la librería, en el
  `expect/actual` que ya existe (`getBannerAdUnitId()` / `getInterstitialAdUnitId()`).

## 5. API pública objetivo

**Hoy hay dos APIs de intersticial conviviendo y ninguna app usa la buena:**
`ads/ui/AdComponents.kt` expone `InterstitialAdTrigger` (genérica, con precarga
automática) que **nadie llama**, mientras el flujo real va por
`ShowPreloadedInterstitial` + `InterstitialAdManager`. La librería se queda con
una sola.

```kotlin
// Banner — el caso simple, una línea
@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String? = null,          // null = el de AdMobConfig
    onAdLoaded: () -> Unit = {},
    onAdFailedToLoad: (String) -> Unit = {},
)

// Intersticial como pantalla — sustituye a AdLoadingScreen
@Composable
fun InterstitialAdScreen(
    onFinished: () -> Unit,            // se llama al cerrar, al fallar y si está desactivado
    loading: @Composable () -> Unit = { DefaultAdLoading() },
)

// Intersticial bajo demanda — para mostrarlo en un momento arbitrario
@Composable
fun rememberInterstitialAd(): InterstitialAdController
// controller.preload() / controller.show(onDismissed) / controller.isReady()

// Arranque (Android)
AdMobKMP.initializeAds(context)        // envuelve MobileAds.initialize + precarga
```

Diferencia clave respecto a hoy: `AdLoadingScreen` recibe un `Navigator` por
Koin y navega él mismo a `Routes.Home`. Eso es acoplamiento con la app y no puede
viajar a la librería. Se sustituye por el callback `onFinished`, y la navegación
la decide quien consume.

`DefaultAdLoading` se expone como parámetro para que cada app ponga su propio
indicador sin depender del `FullScreenLoading` de ListaCompra.

## 6. Estructura del repo

Calcada de `SignInKMP`:

```
AdMobKMP/
├── settings.gradle.kts          rootProject.name = "AdMobKMP"; include(":admob-kmp")
├── build.gradle.kts             plugins apply false
├── gradle.properties
├── gradle/libs.versions.toml
├── jitpack.yml
├── Package.swift                producto AdMobKMPSwift
├── AdMobKMPSwift/
│   └── Sources/AdMobKMPSwift/
│       ├── AdMobCallbackHelper.swift
│       ├── AdMobBannerView.swift
│       ├── AdMobInterstitialBridge.swift   ← unificado con AdPreloader.swift
│       └── AdMobNotifications.swift        ← nombres de notificación como constantes
├── admob-kmp/                   módulo KMP publicable
│   └── src/{commonMain,androidMain,iosMain}/
├── iosApp/                      app de ejemplo (opcional, como en SignInKMP)
├── README.md
├── SETUP_GUIDE.md
└── LICENSE.md
```

## 7. Publicación en JitPack

`jitpack.yml` idéntico al de `SignInKMP`:

```yaml
jdk:
  - openjdk17

install:
  - ./gradlew publishToMavenLocal
```

En `admob-kmp/build.gradle.kts`, al final:

```kotlin
group = "com.github.BonyGoD"
version = "1.0.0"
```

JitPack convierte eso en `com.github.BonyGoD.AdMobKMP:admob-kmp:1.0.0` — el grupo
lleva el nombre del repo por la convención de multi-módulo, igual que
`com.github.BonyGoD.SignInKMP:signin-kmp`.

Requisitos que **no se pueden olvidar** (los tiene SignInKMP y son los que hacen
que el artefacto sirva):

- `id("maven-publish")` en el módulo.
- `androidTarget { publishLibraryVariants("release") }`.
- `android { publishing { singleVariant("release") { withSourcesJar(); withJavadocJar() } } }`.
- Los tres targets iOS declarados: `iosArm64()`, `iosSimulatorArm64()`, `iosX64()`.
- **`api(...)` y no `implementation(...)`** para `play-services-ads` en
  `androidMain`: si no, quien consuma la librería no tendrá el SDK de AdMob en el
  classpath. SignInKMP usa `api` para todas sus dependencias Android por esto
  mismo.

Consumo desde cualquier proyecto:

```kotlin
// settings.gradle.kts — ListaCompra ya lo tiene
maven { url = uri("https://jitpack.io") }

// libs.versions.toml
admob-kmp = "1.0.0"
bonygod-admobkmp = { module = "com.github.BonyGoD.AdMobKMP:admob-kmp", version.ref = "admob-kmp" }
```

Y en Xcode, `XCRemoteSwiftPackageReference` a
`https://github.com/BonyGoD/AdMobKMP` — que es lo que ya se hace con SignInKMP y
CrashlyticsKMP, y sustituye al `XCLocalSwiftPackageReference "../AdMobKMPSwift"`
actual.

## 8. iOS: unificar los dos puentes

Hoy hay dos implementaciones de intersticial en iOS:

- `AdMobKMPSwift/Sources/.../AdMobInterstitialBridge.swift` — empaquetada, **no
  se usa**.
- `iosApp/iosApp/AdPreloader.swift` — en el target de la app, **es la que
  trabaja**: escucha `AdPreloaderPreloadRequested`, `AdPreloaderShowRequested`,
  `AdPreloaderIsReadyRequested` y responde con `AdPreloaderAdShown`,
  `AdPreloaderAdDismissed`, `AdPreloaderShowFailed`.

De `AdMobKMPSwift` la app solo consume `AdMobCallbackHelper.shared`
(`iOSApp.swift:23`).

**Decisión:** se conserva la lógica de `AdPreloader.swift`, que es la probada, y
se mete dentro del paquete fusionándola con `AdMobInterstitialBridge`. Los nombres
de notificación pasan a constantes compartidas en un solo fichero para que Kotlin
y Swift no puedan desincronizarse por un typo — hoy son literales sueltos
repetidos en `ShowPreloadedInterstitial.ios.kt` y en `AdPreloader.swift`.

`Package.swift` declara la dependencia que la app ya tiene resuelta:

```swift
.package(url: "https://github.com/googleads/swift-package-manager-google-mobile-ads.git", from: "12.0.0")
```

## 9. Lo que sigue siendo responsabilidad de quien consume

Se documenta en `SETUP_GUIDE.md`; la librería no puede hacerlo por él:

- **Android**: `com.google.android.gms.ads.APPLICATION_ID` como `meta-data` en el
  manifiesto. No se declara desde la librería con un placeholder: obligaría a
  todo consumidor a definirlo aunque no use anuncios todavía.
- **iOS**: `GADApplicationIdentifier`, `SKAdNetworkItems` y
  `NSUserTrackingUsageDescription` en el `Info.plist`.
- **Los ad unit IDs**, vía `AdMobConfig`. Cada app los saca de donde quiera
  (`local.properties` + BuildConfig, remote config, lo que sea).

## 10. Fases

### Fase 1 — Esqueleto del repo
Crear `BonyGoD/AdMobKMP` copiando la estructura de `SignInKMP`: `settings.gradle.kts`,
`build.gradle.kts` raíz, `gradle.properties`, `libs.versions.toml`, wrapper,
`jitpack.yml`, `LICENSE.md`. Módulo `admob-kmp` vacío que compile.

### Fase 2 — Migrar el Kotlin
Mover los ficheros de §3 con los paquetes renombrados de
`dev.bonygod.listacompra.ads` a `dev.bonygod.admob.kmp`. Sin cambios de lógica
todavía, solo que compile fuera de la app.

### Fase 3 — Configuración
Sustituir `AdConstants` por `AdMobConfig` + `AdMobKMP.configure()`. Quitar toda
referencia a `BuildConfig`. Es el trabajo real de la migración.

### Fase 4 — Consolidar la API
Unificar las dos APIs de intersticial en `InterstitialAdScreen` +
`rememberInterstitialAd()`. Desacoplar del `Navigator` y de `FullScreenLoading`.

### Fase 5 — Swift
Fusionar `AdPreloader.swift` dentro del paquete, extraer las constantes de
notificación, escribir `Package.swift`.

### Fase 6 — Publicar
Tag `1.0.0`, forzar el build en jitpack.io, y **verificar que aparecen los tres
klibs de iOS** en el artefacto, no solo el AAR. Si faltan, el consumo desde iOS
fallará al enlazar y hay que revisar la versión de Kotlin y los targets antes de
seguir.

### Fase 7 — Migrar ListaCompra
- Borrar `composeApp/src/*/ads/`, los `ShowPreloadedInterstitial*` y
  `AdLoadingScreen.kt`.
- Borrar `iosApp/iosApp/AdPreloader.swift` y el paquete local `AdMobKMPSwift/`.
- Sustituir `XCLocalSwiftPackageReference` por `XCRemoteSwiftPackageReference`.
- Añadir la dependencia Gradle y llamar a `AdMobKMP.configure(...)` en
  `ListaCompraApp.onCreate()` y en `iOSApp.swift`, alimentándola desde el
  `BuildConfig` que ya existe.
- El banner de `HomeContent.kt:163` pasa a la versión de la librería.
- El intersticial se queda desactivado (`interstitialEnabled = false`), que es la
  decisión de `PLAN-ONBOARDING-SIN-REGISTRO.md` §Fase 2.

## 11. Riesgos

- **Los klibs de iOS en JitPack.** Está probado con SignInKMP 2.0.1, pero depende
  de la versión de Kotlin. Si se sube a una versión que rompa la
  cross-compilación de klibs desde Linux, la publicación deja de servir para iOS.
  Se verifica en la fase 6, antes de depender de ella.
- **Compose Multiplatform dentro de una librería publicada.** Funciona en
  SignInKMP, que también expone Composables, así que hay precedente. Cuidar que
  las versiones de Compose de librería y app no diverjan.
- **`api` vs `implementation`.** Un `implementation` en `play-services-ads` deja
  al consumidor sin el SDK y el error aparece tarde, al enlazar.
- **Políticas de AdMob.** La librería no debe traer ningún ad unit real. Solo los
  de prueba de Google, que son públicos.
- **Dos apps consumiendo versiones distintas.** ListaCompra y GymRoutine tendrán
  su propia `version.ref`; los cambios rompedores exigen subir mayor, no parchear
  en sitio.

## 12. Criterios de aceptación

1. `./gradlew publishToMavenLocal` genera AAR + los tres klibs de iOS.
2. JitPack construye el tag y sirve `com.github.BonyGoD.AdMobKMP:admob-kmp:1.0.0`.
3. Un proyecto KMP limpio muestra un banner con **una llamada a `BannerAd()`** tras
   un único `AdMobKMP.configure(...)`.
4. Lo mismo con `InterstitialAdScreen(onFinished = ...)`.
5. Con `interstitialEnabled = false` no se hace ninguna petición de intersticial y
   `onFinished` se llama de inmediato.
6. Con `useTestAds = true` funciona sin configurar ningún ID.
7. ListaCompra compila y funciona en Android **y** en iOS consumiendo la librería,
   sin rastro de `ads/` en `composeApp`.
8. El paquete Swift se resuelve por SPM remoto desde el repo, sin referencia local.

## 13. Cómo trabajamos

Igual que en el plan de onboarding: subagente desarrolla fase a fase, yo reviso
el diff, el usuario compila y prueba. Ni yo ni el subagente ejecutamos Gradle.
