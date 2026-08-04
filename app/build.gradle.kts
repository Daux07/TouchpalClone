plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.daux.t9keyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.daux.t9keyboard"
        minSdk = 26
        targetSdk = 35

        // La versione **è** il numero dello step di DEVELOPMENT.md, non una numerazione
        // parallela: provando sul telefono si legge "DauxPal 3.6" nel selettore tastiere e
        // si sa esattamente a che punto del log corrisponde ciò che si ha in mano.
        versionCode = 360
        versionName = "3.6"

        // Nome unico e derivato: il nome dell'app e l'etichetta della tastiera portano
        // sempre la versione, e non possono restare indietro perché non sono scritti a
        // mano da nessuna parte (per questo non stanno più in strings.xml).
        //
        // "DauxPal" dallo Step 3.5, scelto dall'utente. "T9" diceva la tecnica, non il
        // prodotto — e nel selettore tastiere di Android si finisce accanto a nomi propri,
        // dove una sigla si legge come un segnaposto. L'`applicationId` **non** cambia:
        // cambiarlo farebbe disinstallare e reinstallare l'app, e con essa il dizionario
        // personale che l'utente ha costruito scrivendo.
        resValue("string", "app_name", "DauxPal $versionName")
        resValue("string", "ime_label", "DauxPal $versionName")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")

    // Personal dictionary (learned words) — Phase 1.5.
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
}
