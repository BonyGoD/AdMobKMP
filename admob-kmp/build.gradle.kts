import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Sin iosX64, el simulador de los Mac Intel: Compose Multiplatform 1.11 y lifecycle
    // 2.11 ya no publican artefactos para esa plataforma, y con el target declarado
    // Gradle no resuelve compose.runtime. Asi fallo la build de la 1.0.0 en JitPack.
    // ListaCompra lo quito por lo mismo (PLAN-ONBOARDING-SIN-REGISTRO, seccion 11).
    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    // El framework solo se configura en macOS: enlazarlo requiere la toolchain de
    // Apple. En Linux (JitPack) los targets siguen declarados —de ahí salen los
    // dos klibs que consume el lado iOS— pero no se intenta el enlazado.
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        iosTargets.forEach { target ->
            target.binaries.framework {
                baseName = "AdMobKMP"
                isStatic = true
                binaryOption("bundleId", "dev.bonygod.admob.kmp")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)

                implementation(libs.androidx.lifecycle.runtimeCompose)

                // ShowPreloadedInterstitial usa kotlinx.coroutines.delay en
                // androidMain e iosMain. Se declara explícita porque hoy llega
                // transitiva vía compose.runtime, y eso es frágil: no debe
                // depender de lo que Compose decida arrastrar en el futuro.
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)

                // AdMob — api y no implementation: quien consuma la librería
                // necesita el SDK de AdMob en su classpath.
                api(libs.play.services.ads)
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
        }

        val iosTest by creating {
            dependsOn(commonTest)
        }

        iosTargets.forEach { target ->
            target.compilations["main"].defaultSourceSet.dependsOn(iosMain)
            target.compilations["test"].defaultSourceSet.dependsOn(iosTest)
        }
    }
}

android {
    namespace = "dev.bonygod.admob.kmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

// Información para publicación en JitPack
group = "com.github.BonyGoD"
version = "1.0.1"
