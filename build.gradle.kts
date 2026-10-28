// Los plugins se declaran aquí sin aplicarlos (apply false) para que todos los
// módulos usen la misma versión. Cada módulo aplica los que necesita.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose) apply false
}
