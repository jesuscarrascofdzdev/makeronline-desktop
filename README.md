# MakerOnline Desktop

App de escritorio para subir y descargar modelos 3D de [MakerOnline](https://www.makeronline.com) sin terminal, sin Maven y sin IntelliJ. Está basada en la herramienta de terminal [makerOnlineUploader](https://github.com/jesuscarrascofdzdev/makerOnlineUploader).

- Funciona entera **sin IA**. La IA es opcional y cada persona usa su propia clave o una IA local (Ollama).
- La sesión de MakerOnline (perfil de Chrome) **nunca sale de tu ordenador**.
- Los captchas **no se saltan**: se enseña la ventana para que los resuelva la persona.

> **Estado: fase 1 en curso.** El código de la herramienta de terminal está en el módulo `core` y se está preparando para usarlo desde la app (paso 1a hecho: las rutas están en `Rutas`). La app de escritorio todavía es solo una ventana de prueba.

---

## Estructura

```
makeronline-desktop/
├── core/       Lógica de MakerOnline: sesión, subir, descargar, likes, registro, candado (Java)
├── desktop/    App de escritorio con Compose for Desktop (Kotlin)
├── gradle/     Versiones de las librerías (libs.versions.toml) y el wrapper de Gradle
└── .run/       Configuraciones de ejecución ya preparadas para IntelliJ
```

## Para desarrollar

**Qué necesitas**
- IntelliJ IDEA.
- JDK 21. Si no lo tienes, Gradle lo descarga solo.
- Google Chrome instalado.

**Abrir el proyecto**
1. En IntelliJ: *File → Open* y elige esta carpeta.
2. Espera a que Gradle termine de sincronizar. La primera vez tarda porque descarga las librerías.

**Ejecutar**

Arriba a la derecha, en el desplegable de ejecución, está la carpeta **MakerOnline**:

| Configuración | Qué hace |
|---|---|
| Guardar sesión | Abre Chrome para iniciar sesión en MakerOnline. Solo la primera vez o cuando caduque |
| Descargar (simular) | Lista lo que descargaría, sin descargar nada |
| Descargar | Descarga los modelos nuevos de los usuarios de `usuarios.json` |
| Subir (revisar) | Sube los modelos de `subidas/pendientes/` y se para antes de guardar cada borrador |
| Subir | Sube los modelos de `subidas/pendientes/` como borrador |
| Codegen | Chrome con sesión y el Playwright Inspector, para grabar selectores |
| App de escritorio | Abre la app de escritorio |

Los programas de terminal se ejecutan desde la raíz del repo. Ahí se crean `chrome-profile/`, `usuarios.json`, `descargados.txt`, `subidas/` y `modelos-descargados/`. **Nada de eso se sube a git.**

**Desde la terminal (opcional)**
- Compilar todo: `gradlew build`
- Abrir la app: `gradlew :desktop:run`

---

## Para asistentes de IA

- **Stack:** Gradle 8.14.3 (Kotlin DSL) con varios módulos. Versiones en `gradle/libs.versions.toml`: Kotlin 2.2.20, Compose Multiplatform 1.9.3, Playwright para Java 1.52.0 y Jackson 2.18.2. JDK 21 (toolchain).
- **`core`:** paquete `dev.jefe.makeronline`. El código heredado está en Java (`src/main/java`) y las piezas nuevas en Kotlin (`src/main/kotlin`). Programas con `main`: `SaveSession`, `Uploader`, `Downloader` y `Codegen`. Todas las rutas de datos salen de `Rutas`. Los programas de terminal usan `Rutas.carpetaActual()`, es decir, el directorio de trabajo, que debe ser la raíz del repo.
- **`desktop`:** Kotlin + Compose for Desktop, paquete `dev.jefe.makeronline.desktop`. Todos los textos de la interfaz están en `Textos.kt`.
- **Reglas:** textos y comentarios en español. No inventar selectores ni endpoints de MakerOnline. Pausa de 10 a 30 s entre descargas y nunca en paralelo. No saltar captchas.
- **Compilar:** `gradlew build`. No hay tests todavía.
