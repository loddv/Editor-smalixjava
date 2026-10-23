plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.smalixjava.editor"
    compileSdk = 35

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.smalixjava.editor"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation("androidx.documentfile:documentfile:1.0.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    val editorVersion = "0.24.4"
    implementation("io.github.rosemoe:editor:$editorVersion")
    implementation("io.github.rosemoe:language-textmate:$editorVersion")
}
