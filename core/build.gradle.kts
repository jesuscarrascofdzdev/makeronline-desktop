// core: la lógica de MakerOnline (sesión, subir, descargar, likes, registro, candado).
// Fase 0: es el código de makeronline-agent tal cual, en Java.
plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Automatización del navegador (trae también Gson, que usa el Downloader)
    implementation(libs.playwright)
    // Leer y escribir info.json y usuarios.json
    implementation(libs.jackson.databind)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
