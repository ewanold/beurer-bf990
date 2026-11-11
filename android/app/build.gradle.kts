plugins { id("com.android.application") }

android {
    namespace = "org.beurer.bf990"
    compileSdk = 36
    defaultConfig {
        applicationId = "org.beurer.bf990"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    val sharedDebugKey = rootProject.file("../database/signing/debug.keystore")
    signingConfigs {
        getByName("debug") {
            storeFile = sharedDebugKey
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}
