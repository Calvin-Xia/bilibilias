plugins {
    alias(libs.plugins.bilibilias.multiplatform.library)
    alias(libs.plugins.bilibilias.multiplatform.koin)
    alias(libs.plugins.kotlin.plugin.serialization)
}

kotlin {
    android {
        namespace = "com.imcys.bilibilias.network"
        withHostTest {}
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:common"))
            implementation(project(":core:database"))
            implementation(project(":core:datastore"))
            implementation(libs.okio)
            api(libs.ktor.client.core)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.serialization.kotlinx.json)
            api(libs.ktor.serialization.kotlinx.protobuf)
            api(libs.ksoup.network)
            api(libs.ktor.client.logging)
            api(libs.kotlinx.io.core)
            api(libs.kotlinx.io.bytestring)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            api(libs.ktor.client.okhttp)
        }

        iosMain.dependencies {
            api(libs.ktor.client.darwin)
        }
    }
}
