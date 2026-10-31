// desktop: la aplicación de escritorio (Compose for Desktop).
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

// Fase 2: al lanzar la app con "run", usa la misma carpeta de datos que los programas de
// terminal (la raíz del repo): comparte la sesión de Chrome, usuarios.json y descargados.txt.
// En la fase 4 pasará a la carpeta de datos del usuario.
tasks.withType<JavaExec>().configureEach {
    if (name == "run") systemProperty("makeronline.datos", rootDir.absolutePath)
}
