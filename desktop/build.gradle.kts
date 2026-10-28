// desktop: la aplicación de escritorio (Compose for Desktop).
// Fase 0: solo abre una ventana de prueba.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "dev.jefe.makeronline.desktop.MainKt"
        // El instalador (.msi) se configura en la fase 4
    }
}
