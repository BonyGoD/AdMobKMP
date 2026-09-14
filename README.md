# AdMobKMP

Anuncios de AdMob para Kotlin Multiplatform + Compose Multiplatform, en Android y iOS.

El objetivo es que integrar un anuncio sea esto y nada más:

```kotlin
// Banner
BannerAd(modifier = Modifier.fillMaxWidth())

// Intersticial como pantalla completa
InterstitialAdScreen(onFinished = { navigator.clearAndNavigateTo(Routes.Home) })
```

Sin pasar ad unit IDs en cada llamada, sin montar el puente de iOS a mano y sin
duplicar `expect/actual` en cada proyecto. La configuración se da una vez al
arrancar y los componentes la leen de ahí.

## Instalación

La librería viaja en dos mitades, porque Kotlin/Native no puede enlazar el SDK
de Google Mobile Ads para iOS: el Kotlin va por JitPack y la mitad Swift por SPM
desde este mismo repositorio.

### Gradle (Android + común)

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

```toml
# gradle/libs.versions.toml
[versions]
admob-kmp = "1.0.0"

[libraries]
bonygod-admobkmp = { module = "com.github.BonyGoD.AdMobKMP:admob-kmp", version.ref = "admob-kmp" }
```

```kotlin
// composeApp/build.gradle.kts
commonMain.dependencies {
    implementation(libs.bonygod.admobkmp)
}
```

### Swift Package Manager (iOS)

En Xcode, *File → Add Package Dependencies* con la URL de este repositorio:

```
https://github.com/BonyGoD/AdMobKMP
```

Producto a añadir al target: **`AdMobKMPSwift`**.

## Configuración

Una sola vez, en el arranque, antes de mostrar cualquier componente:

```kotlin
AdMobKMP.configure(
    AdMobConfig(
        androidBannerId = BuildConfig.ADMOB_ANDROID_BANNER,
        androidInterstitialId = BuildConfig.ADMOB_ANDROID_INTERSTITIAL,
        iosBannerId = BuildConfig.ADMOB_IOS_BANNER,
        iosInterstitialId = BuildConfig.ADMOB_IOS_INTERSTITIAL,
        useTestAds = false,
        interstitialEnabled = true,
    )
)
```

Los ad unit IDs son **tuyos**: la librería no trae ninguno real, solo los de
prueba públicos de Google. De dónde los saques (BuildConfig y `local.properties`,
remote config, lo que sea) es cosa de tu app.

Con `useTestAds = true` funciona sin configurar ningún ID: sirve los de prueba de
Google. Y si dejas un ID de producción en blanco, la librería cae al de prueba y
avisa por consola, en vez de pasarle una cadena vacía al SDK.

### Arranque

```kotlin
// Android — Application.onCreate(), después de configure()
AdMobKMP.initializeAds(context)
```

```swift
// iOS — init de la App
AdMobKMPBridge.start()
```

```kotlin
// iOS — desde Kotlin, después de configure() y de AdMobKMPBridge.start()
AdMobKMP.initializeAds()
```

Además hay que declarar el App ID de AdMob en el manifiesto de Android y en el
`Info.plist` de iOS. Está detallado en [SETUP_GUIDE.md](SETUP_GUIDE.md).

## API

### `BannerAd`

```kotlin
@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String? = null,          // null = el de AdMobConfig
    onAdLoaded: () -> Unit = {},
    onAdFailedToLoad: (String) -> Unit = {},
)
```

### `InterstitialAdScreen`

Intersticial como pantalla completa: pinta un indicador de carga, muestra el
anuncio precargado y llama a `onFinished` **exactamente una vez**, al cerrarse,
al fallar, o de inmediato si el intersticial está desactivado.

```kotlin
@Composable
fun InterstitialAdScreen(
    onFinished: () -> Unit,
    loading: @Composable () -> Unit = { DefaultAdLoading() },
)
```

La navegación la decide quien consume, en `onFinished`. La librería no conoce tu
`Navigator` ni tus rutas.

### `rememberInterstitialAd`

Intersticial bajo demanda, para mostrarlo en un momento arbitrario:

```kotlin
val ad = rememberInterstitialAd()

ad.preload()
if (ad.isReady()) {
    ad.show(onDismissed = { /* ... */ })
}
```

### `AdMobKMP`

```kotlin
AdMobKMP.configure(config: AdMobConfig)
AdMobKMP.config(): AdMobConfig
AdMobKMP.isInterstitialEnabled(): Boolean
AdMobKMP.getBannerAdUnitId(): String        // resuelto por plataforma
AdMobKMP.getInterstitialAdUnitId(): String  // resuelto por plataforma
AdMobKMP.initializeAds(context)             // Android
AdMobKMP.initializeAds()                    // iOS
```

## El interruptor del intersticial

Con `interstitialEnabled = false` la librería **no hace ninguna petición de
intersticial** por ninguna vía: no precarga, no muestra, `isReady()` devuelve
`false`, `show()` llama a `onDismissed()` de inmediato y `InterstitialAdScreen`
llama a `onFinished()` sin esperar. El banner no depende de este flag.

## Licencia

Software propietario. Ver [LICENSE.md](LICENSE.md).
