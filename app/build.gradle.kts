import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

fun escapeBuildConfig(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "")
        .replace("\r", "")
}

android {
    namespace = "com.mindlab.worky"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.mindlab.worky"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localFile.inputStream().use { localProps.load(it) }
        }
        val supabaseUrl = localProps.getProperty("SUPABASE_URL") ?: ""
        val supabaseAnon = localProps.getProperty("SUPABASE_ANON_KEY") ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"${escapeBuildConfig(supabaseUrl)}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${escapeBuildConfig(supabaseAnon)}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
