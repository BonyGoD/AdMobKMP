# Guía de integración

Lo que AdMobKMP **no** puede hacer por ti, y por qué.

La librería resuelve los ad unit IDs, los componentes de UI y el puente con el
SDK nativo. Lo que queda aquí son cosas que viven en la configuración de *tu*
app: si la librería las impusiera, obligaría a todo consumidor a definirlas
aunque todavía no muestre ningún anuncio.

---

## 1. Android — App ID en el manifiesto

El SDK de Google Mobile Ads **falla al arrancar** si no encuentra este
`meta-data`. No se declara desde la librería a propósito: con un placeholder
obligaría a cualquiera que la incluya a definirlo, aun sin usar anuncios.

```xml
<!-- composeApp/src/androidMain/AndroidManifest.xml -->
<application>
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY" />
</application>
```

Ojo: el App ID lleva `~`, y los ad unit IDs llevan `/`. No son lo mismo y no son
intercambiables.

Para desarrollo, el App ID de prueba público de Google es
`ca-app-pub-3940256099942544~3347511713`.

## 2. iOS — claves en el `Info.plist`

```xml
<key>GADApplicationIdentifier</key>
<string>ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY</string>

<key>NSUserTrackingUsageDescription</key>
<string>Usamos tu identificador para mostrarte anuncios más relevantes.</string>

<key>SKAdNetworkItems</key>
<array>
    <dict>
        <key>SKAdNetworkIdentifier</key>
        <string>cstr6suwn9.skadnetwork</string>
    </dict>
    <!-- La lista completa y actualizada está en la documentación de AdMob -->
</array>
```

La lista de `SKAdNetworkItems` la publica Google y cambia con el tiempo:
consúltala en la documentación oficial de AdMob para iOS en lugar de copiarla de
aquí.

`NSUserTrackingUsageDescription` es obligatoria si vas a pedir permiso de
seguimiento (ATT). Sin ella, App Store rechaza la app.

## 3. Los ad unit IDs

Son tuyos, de tu cuenta de AdMob, y nunca viajan dentro de la librería. Se
entregan una sola vez:

```kotlin
AdMobKMP.configure(
    AdMobConfig(
        androidBannerId = ...,
        androidInterstitialId = ...,
        iosBannerId = ...,
        iosInterstitialId = ...,
    )
)
```

De dónde los lea tu app es indiferente para la librería. El patrón habitual es
`local.properties` (fuera de git) más un `BuildConfig` generado:

```kotlin
// composeApp/build.gradle.kts
buildConfig {
    val props = Properties()
    rootProject.file("local.properties").takeIf { it.exists() }?.let { props.load(it.reader()) }
    buildConfigField("ADMOB_ANDROID_BANNER", props.getProperty("ADMOB_ANDROID_BANNER") ?: "")
    // ... el resto
}
```

Este `buildConfig` va en **tu** app, no en la librería: un artefacto publicado
con los IDs del autor horneados dentro sería un error de seguridad y de política
de AdMob.

## 4. Orden de arranque

El orden importa: `configure()` antes de `initializeAds()`, y en iOS el puente
Swift antes de que Kotlin pida cualquier precarga.

### Android

```kotlin
class MiApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AdMobKMP.configure(AdMobConfig(/* ... */))
        AdMobKMP.initializeAds(this)
    }
}
```

### iOS

```swift
import AdMobKMPSwift

@main
struct iOSApp: App {
    init() {
        AdMobKMPBridge.start()   // arranca el SDK y los puentes con Kotlin
    }

    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
```

Y desde el Kotlin común de iOS, tras `configure()`:

```kotlin
AdMobKMP.initializeAds()
```

Si `AdMobKMPBridge.start()` no se llama, las notificaciones que manda Kotlin no
tienen a nadie escuchando al otro lado: no hay error, simplemente no aparece
ningún anuncio. Es el fallo de integración más habitual en iOS.

## 5. Permiso de red (Android)

`play-services-ads` ya declara `android.permission.INTERNET` y
`ACCESS_NETWORK_STATE` por *manifest merging*. No hace falta añadirlos.

## 6. Comprobar que funciona

1. Arranca con `useTestAds = true` y sin configurar ningún ID. Debe aparecer un
   banner de prueba de Google con una sola llamada a `BannerAd()`.
2. Pon tus IDs reales y `useTestAds = false`. Si ves en consola
   `⚠️ [AdMobKMP] ... no configurado`, es que un ID llegó vacío y estás viendo
   anuncios de prueba.
3. Con `interstitialEnabled = false`, `InterstitialAdScreen` debe llamar a
   `onFinished` de inmediato y no debe haber ninguna petición de intersticial en
   los logs.

Los anuncios reales pueden tardar horas en servirse tras crear un ad unit nuevo,
y AdMob no siempre tiene inventario: un banner que no aparece con IDs recién
creados no es necesariamente un fallo de integración. Comprueba primero con los
IDs de prueba.
