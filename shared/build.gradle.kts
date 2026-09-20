plugins {
    alias(libs.plugins.bilibilias.multiplatform.library)
    alias(libs.plugins.bilibilias.multiplatform.koin)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildconfig)
}

kotlin {
    android {
        namespace = "com.imcys.bilibilias.shared"
        androidResources {
            enable = true
        }
        withHostTest {}
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ASShared"
            isStatic = true
            export(project(":core:data"))
            transitiveExport = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.device.compat)
            implementation(libs.androidx.ui.tooling.preview)
            implementation(libs.androidx.documentfile)
            implementation(libs.ffmpeg.kit.x6kb)
            implementation(libs.androidx.navigation3.runtime)
            implementation(libs.compose.cloudy)
        }

        commonMain {
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            kotlin.srcDir("build/generated/compose/resourceGenerator/kotlin/commonMainResourceAccessors")
            kotlin.srcDir("build/generated/compose/resourceGenerator/kotlin/commonResClass")
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.androidx.material.icons.extended.kmp)
                implementation(libs.kmp.androidx.lifecycle.runtimeCompose)
                implementation(libs.kmp.androidx.lifecycle.viewmodel)
                implementation(libs.kotlinx.datetime)
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
                implementation(libs.jetbrains.material3.adaptive)
                implementation(libs.jetbrains.material3.adaptive.layout)
                implementation(libs.jetbrains.material3.adaptive.navigation)
                implementation(libs.jetbrains.material3.adaptive.navigation3)
                implementation(libs.paging.compose)
                implementation(libs.koin.compose.kmp)
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.confettikit)
                api(project(":core:data"))
                api(project(":core:common"))
                api(project(":core:ui"))
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }

}



buildConfig {
    className("ASBuildConfig")

    val enabledPlayAppMode: String by project
    val enabledAnalytics: String by project
    fun Project.optionalStringProperty(name: String): String = findProperty(name)?.toString() ?: ""
    val baiduStatId: String = project.optionalStringProperty("as.baidu.stat.id")
    val githubOrg: String = project.optionalStringProperty("as.github.org")
    val githubRepository: String = project.optionalStringProperty("as.github.repository")
    val gitCommitHash: String = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
    }.standardOutput.asText.get().trim()

    buildConfigField("String", "BAIDU_STAT_ID", """"$baiduStatId"""".trimIndent())
    buildConfigField("String", "GIT_COMMIT_HASH", """"$gitCommitHash"""".trimIndent())
    buildConfigField("String","GITHUB_ORG",""""$githubOrg"""".trimIndent())
    buildConfigField("String","GITHUB_REPOSITORY",""""$githubRepository"""".trimIndent())
    buildConfigField("boolean", "ENABLED_PLAY_APP_MODE", enabledPlayAppMode)
    buildConfigField("boolean", "ENABLED_ANALYTICS", enabledAnalytics)
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
