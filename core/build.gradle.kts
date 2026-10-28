// core: la lógica de MakerOnline (sesión, subir, descargar, likes, registro, candado).
// El código heredado de makeronline-agent está en Java (src/main/java) y las piezas
// nuevas en Kotlin (src/main/kotlin). Java y Kotlin se usan entre sí sin problema.
plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    // Java 21 para Kotlin y para Java
    jvmToolchain(21)
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
