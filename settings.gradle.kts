// MakerOnline Desktop: un repo con varios módulos.
//   core    → lógica de MakerOnline (sesión, subir, descargar, likes...). Sin interfaz.
//   desktop → la aplicación de escritorio (Compose for Desktop).

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    // Si no tienes instalado el JDK que pide el proyecto (21), Gradle lo descarga solo
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google() // Compose usa algunas librerías androidx que solo están aquí
    }
}

rootProject.name = "makeronline-desktop"
include("core", "desktop")
