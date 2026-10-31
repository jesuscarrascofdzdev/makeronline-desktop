package dev.jefe.makeronline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * El agente.
 *
 * Recorre subidas/pendientes/, valida cada carpeta, rellena el formulario de
 * MakerOnline y lo guarda como BORRADOR (draft). Después mueve la carpeta a
 * subidas/subidos/ (con la fecha delante) o a subidas/errores/ (con error.txt
 * y una captura del momento del fallo).
 *
 * Argumentos (opcionales):
 *   --revisar   se para justo antes de guardar el borrador para que mires el
 *               formulario. Pulsa "Resume" en el Inspector para continuar.
 *   --esperar   si el Downloader (u otro programa del agente) está en marcha,
 *               espera a que termine en vez de salir.
 *   --diagnostico  TEMPORAL: graba en subidas/diagnostico_<fecha>_<modelo>.txt cómo
 *               avanzan las subidas y qué pasa al guardar, y espera a que pulses
 *               ENTER antes de guardar (ver DiagnosticoSubida).
 *   nombre ...  solo procesa esas carpetas de pendientes/ (sin nombres, todas).
 *               Ejemplo: --revisar llavero-gato
 */
public class Uploader {

    private static final String UPLOAD_URL = "https://www.makeronline.com/en/upload";

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");
    private static final ObjectMapper JSON = new ObjectMapper();
    /** Intentos (uno por segundo) para mover una carpeta que Windows tiene bloqueada. */
    private static final int INTENTOS_MOVER = 5;

    /**
     * Opciones de una ejecución.
     *
     * @param revisar  se para justo antes de guardar cada borrador (--revisar)
     * @param carpetas solo estas carpetas de pendientes/; vacía = todas
     * @param esperar  si otro programa del agente está en marcha, espera a que termine (--esperar)
     * @param diagnostico TEMPORAL: graba cómo avanzan las subidas (--diagnostico)
     */
    public record Opciones(boolean revisar, List<String> carpetas, boolean esperar, boolean diagnostico) {}

    /**
     * Cómo ha ido una ejecución.
     *
     * @param subidos  guardados como borrador
     * @param conError fallaron al subir (están en errores/ con error.txt y una captura)
     * @param invalidos no pasaron la validación (también están en errores/)
     */
    public record Resumen(int subidos, int conError, int invalidos) {}

    /** Dónde están la sesión de Chrome y las carpetas de subidas. */
    private final Rutas rutas;
    /** A quién se le cuenta lo que va pasando (terminal o app). */
    private final Progreso progreso;

    public Uploader(Rutas rutas, Progreso progreso) {
        this.rutas = rutas;
        this.progreso = progreso;
    }

    public static void main(String[] args) throws Exception {
        Opciones op = leerArgs(args);
        Rutas rutas = Rutas.carpetaActual();
        Progreso progreso = new ProgresoConsola();
        // Solo un programa del agente a la vez: comparten chrome-profile/
        try (AgentLock lock = AgentLock.acquire(rutas.getCandado(), "Uploader", op.esperar(), progreso)) {
            new Uploader(rutas, progreso).ejecutar(op);
        } catch (AgenteException e) {
            progreso.error(e.getMessage());
            System.exit(e.getCodigoSalida());
        }
    }

    private static Opciones leerArgs(String[] args) {
        List<String> lista = List.of(args);
        return new Opciones(
                lista.contains("--revisar"),
                lista.stream().filter(a -> !a.startsWith("--")).toList(), // nombres de carpeta
                lista.contains("--esperar"),
                lista.contains("--diagnostico"));
    }

    /**
     * Valida y sube como borrador los modelos de pendientes/.
     *
     * @throws AgenteException si no hay sesión guardada
     */
    public Resumen ejecutar(Opciones op) throws Exception {
        Path pendientes = rutas.getPendientes();
        Files.createDirectories(pendientes);
        Files.createDirectories(rutas.getSubidos());
        Files.createDirectories(rutas.getErrores());

        if (!Files.isDirectory(rutas.getPerfilChrome())) {
            throw new AgenteException("No hay sesión guardada. Ejecuta primero SaveSession.");
        }

        // 1) Validar TODO antes de abrir el navegador
        List<ModelFolder> listos = new ArrayList<>();
        List<Path> carpetas;
        try (Stream<Path> s = Files.list(pendientes)) {
            carpetas = s.filter(Files::isDirectory).sorted().toList();
        }

        // Si se pasan nombres de carpeta, solo se procesan esas
        List<String> nombres = op.carpetas();
        if (!nombres.isEmpty()) {
            for (String n : nombres) {
                if (!Files.isDirectory(pendientes.resolve(n))) progreso.error("No existe en pendientes: " + n);
            }
            carpetas = carpetas.stream().filter(d -> nombres.contains(d.getFileName().toString())).toList();
        }

        if (carpetas.isEmpty()) {
            progreso.info("No hay nada en " + pendientes.toAbsolutePath());
            return new Resumen(0, 0, 0);
        }
        int invalidos = 0;
        for (Path dir : carpetas) {
            try {
                listos.add(ModelFolder.load(dir));
                progreso.correcto(dir.getFileName() + " validado");
            } catch (IllegalStateException | IOException e) {
                progreso.error(dir.getFileName() + ":\n   " + e.getMessage().replace("\n", "\n   "));
                moverAErrores(dir, e.getMessage());
                invalidos++;
            }
        }
        if (listos.isEmpty()) {
            progreso.info("Ningún modelo válido. Revisa " + rutas.getErrores() + ".");
            return new Resumen(0, 0, invalidos);
        }

        // 2) Subir los válidos
        int ok = 0, ko = 0;
        try (Playwright pw = Playwright.create();
             BrowserContext ctx = Browsers.openChrome(pw, rutas.getPerfilChrome(), 150)) {
            ctx.setDefaultTimeout(30_000);

            for (int i = 0; i < listos.size(); i++) {
                ModelFolder m = listos.get(i);
                progreso.modelo(i + 1, listos.size(), m.name());
                Page page = ctx.newPage();
                DiagnosticoSubida diag = op.diagnostico()
                        ? new DiagnosticoSubida(page, rutas.getSubidas().resolve(
                                "diagnostico_" + LocalDateTime.now().format(STAMP) + "_" + m.name() + ".txt"), progreso)
                        : null;
                boolean guardado = false;
                try {
                    subir(page, m, op.revisar(), diag);
                    guardado = true;
                    captura(page, m.dir().resolve("resultado.png"));
                } catch (Exception e) {
                    if (diag != null) diag.marca("ERROR: " + e);
                    progreso.error("Error: " + e.getMessage());
                    captura(page, m.dir().resolve("error.png"));
                    // Primero se cierra la pestaña: así Chrome deja de leer los archivos y Windows deja moverlos
                    cerrar(page, diag);
                    moverAErrores(m.dir(), e.toString());
                    ko++;
                } finally {
                    cerrar(page, diag);
                }
                if (guardado) {
                    ok++;
                    try {
                        Path destino = mover(m.dir(), rutas.getSubidos());
                        progreso.correcto("Borrador guardado → " + destino);
                    } catch (IOException e) {
                        // El borrador YA está en MakerOnline: si la carpeta se queda en pendientes/,
                        // la próxima vez se subiría otra vez
                        progreso.error("El borrador se ha guardado en MakerOnline, pero no he podido mover "
                                + m.dir() + " a subidos/: " + e.getMessage()
                                + "\n  Muévela tú (o bórrala de pendientes/) para que no se vuelva a subir.");
                    }
                }
            }
        }
        progreso.info(String.format("%nTerminado: %d subidos, %d con error.", ok, ko));
        return new Resumen(ok, ko, invalidos);
    }

    /** Cierra la pestaña (y el diagnóstico, si lo hay). Se puede llamar varias veces. */
    private static void cerrar(Page page, DiagnosticoSubida diag) {
        if (diag != null) diag.close();
        if (!page.isClosed()) page.close();
    }

    /** Cierra el aviso de cookies si aparece: tapa la ventana de licencia y bloquea los clics. */
    private void cerrarCookies(Page page) {
        Locator boton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Necessary Only"));
        try {
            boton.waitFor(new Locator.WaitForOptions().setTimeout(3_000));
            boton.click();
            paso("Aviso de cookies cerrado");
        } catch (TimeoutError e) {
            // no hay aviso de cookies
        }
    }

    /**
     * Rellena el formulario completo y guarda como borrador.
     *
     * @param diag TEMPORAL: si no es null, graba cómo avanzan las subidas y espera ENTER antes de guardar
     */
    private void subir(Page page, ModelFolder m, boolean revisar, DiagnosticoSubida diag) {
        ModelMeta meta = m.meta();
        if (diag != null) diag.archivos(m.cover().getFileName().toString(), m.model().getFileName().toString());
        // Se crea antes de elegir ningún archivo para que vea todas las subidas.
        // No hace falta cerrarlo: deja de escuchar cuando se cierra la pestaña.
        VigilanteSubidas subidas = new VigilanteSubidas(page, progreso);

        paso("Abriendo formulario");
        page.navigate(UPLOAD_URL);

        // --- Licencia (ventana que sale al entrar) ---
        Locator licencia = page.getByRole(AriaRole.TEXTBOX,
                new Page.GetByRoleOptions().setName("Choose a CC license"));
        try {
            licencia.waitFor(new Locator.WaitForOptions().setTimeout(15_000));
        } catch (TimeoutError e) {
            throw new IllegalStateException(
                    "No se ha cargado el formulario de subida. ¿Ha caducado la sesión? Ejecuta SaveSession.");
        }
        cerrarCookies(page);
        paso("Licencia: " + meta.licenseOrDefault());
        licencia.click();
        page.getByText(meta.licenseOrDefault()).first().click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

        // --- Portada ---
        paso("Portada: " + m.cover().getFileName());
        elegirArchivo(page, page.locator(".el-upload-dragger").first(), m.cover());
        if (diag != null) {
            diag.marca("portada elegida");
            diag.observar("portada", 15);
        }
        subidas.esperar("la portada");

        // --- Título ---
        paso("Título");
        Locator titulo = page.locator(".el-form-item")
                .filter(new Locator.FilterOptions().setHasText("Model Title"))
                .locator("input, textarea").first();
        if (titulo.count() == 0) {
            titulo = page.locator("form").getByRole(AriaRole.TEXTBOX).nth(1); // selector grabado por codegen
        }
        titulo.fill(meta.title());

        // --- Categoría (2 niveles) ---
        paso("Categoría: " + String.join(" > ", meta.category()));
        Locator selectorTipo = page.getByRole(AriaRole.TEXTBOX,
                new Page.GetByRoleOptions().setName("First level classification/"));
        Locator primerNivel = page.getByText(meta.category().get(0),
                new Page.GetByTextOptions().setExact(true));
        // el desplegable a veces no abre con el primer clic: se reintenta
        for (int intento = 1; ; intento++) {
            selectorTipo.scrollIntoViewIfNeeded();
            selectorTipo.click();
            try {
                primerNivel.waitFor(new Locator.WaitForOptions().setTimeout(5_000));
                break;
            } catch (TimeoutError e) {
                if (intento >= 3) throw e;
                page.waitForTimeout(1000);
            }
        }
        for (String nivel : meta.category()) {
            page.getByText(nivel, new Page.GetByTextOptions().setExact(true)).click();
        }

        // --- Tags ---
        paso("Tags: " + tagsLimpios(meta.tags()));
        Locator tags = page.getByRole(AriaRole.TEXTBOX,
                new Page.GetByRoleOptions().setName("*Press the Enter key to"));
        for (String tag : tagsLimpios(meta.tags())) {
            tags.click();
            tags.fill(tag);
            tags.press("Enter");
        }

        // --- Visibilidad y tipo de impresión ---
        paso("Public + " + meta.printTypeOrDefault());
        page.locator("label").filter(new Locator.FilterOptions().setHasText("Public")).click();
        page.locator("label").filter(new Locator.FilterOptions().setHasText(meta.printTypeOrDefault())).click();

        // --- Descripción (editor Quill): se escribe de verdad, línea a línea ---
        paso("Descripción");
        page.locator(".ql-editor").click();
        String[] lineas = meta.description().split("\\R", -1);
        for (int i = 0; i < lineas.length; i++) {
            if (i > 0) page.keyboard().press("Enter");
            if (!lineas[i].isEmpty()) page.keyboard().insertText(lineas[i]);
        }

        // --- Paso 2: archivos ---
        paso("Siguiente paso: archivos");
        if (diag != null) {
            diag.foto("antes de Next Step");
            diag.marca("pulso Next Step");
        }
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next Step, Add Files")).click();
        try {
            page.locator(".el-upload").filter(new Locator.FilterOptions().setHasText("Supported files: stl"))
                    .first().waitFor(new Locator.WaitForOptions().setTimeout(15_000));
        } catch (TimeoutError e) {
            String avisos = String.join(" | ",
                    page.locator(".el-message, .el-form-item__error, .el-message-box").allInnerTexts());
            throw new IllegalStateException("No avanza al paso de archivos. Avisos del formulario: "
                    + (avisos.isBlank() ? "(ninguno)" : avisos));
        }

        if (m.is3mf()) {
            paso("Perfil de impresión (3mf): " + m.model().getFileName());
            elegirArchivo(page, zonaSubida(page, "only supports single 3mf"), m.model());
            if (diag != null) {
                diag.marca("perfil .3mf elegido");
                diag.foto("perfil .3mf");
            }
            // No se espera aquí: el .3mf sigue subiendo mientras se rellena el resto
            // (comprobado que funciona) y se espera a todo junto antes de guardar.

            paso("Título del perfil de impresión");
            Locator tituloPerfil = page.locator(".el-form-item")
                    .filter(new Locator.FilterOptions().setHasText("Print Profile Title"))
                    .locator("input, textarea").first();
            try {
                tituloPerfil.waitFor(new Locator.WaitForOptions().setTimeout(15_000));
            } catch (TimeoutError e) {
                throw new IllegalStateException(
                        "No encuentro el campo 'Print Profile Title'. Graba el selector con Codegen.");
            }
            tituloPerfil.fill(meta.title());

            paso("Imagen del perfil de impresión: " + m.cover().getFileName());
            Locator zonaFotos = page.locator(".el-form-item")
                    .filter(new Locator.FilterOptions().setHasText("Print Profile Pictures"))
                    .locator(".el-upload").first();
            try {
                zonaFotos.waitFor(new Locator.WaitForOptions().setTimeout(15_000));
            } catch (TimeoutError e) {
                throw new IllegalStateException(
                        "No encuentro la zona 'Print Profile Pictures'. Graba el selector con Codegen.");
            }
            elegirArchivo(page, zonaFotos, m.cover());
            if (diag != null) {
                diag.marca("imagen del perfil elegida");
                diag.foto("imagen del perfil");
            }
        }
        paso("Archivo del modelo: " + m.model().getFileName());
        elegirArchivo(page, zonaSubida(page, "Supported files: stl"), m.model());
        if (diag != null) diag.marca("archivo del modelo elegido");

        // --- Esperar a que TODO esté subido (si algo falla o se atasca, no se guarda) ---
        subidas.esperar("los archivos");
        if (diag != null) diag.esperarEnter(20);

        if (revisar) {
            progreso.info("   ⏸ Revisa el formulario. Pulsa 'Resume' en el Inspector para guardar el borrador.");
            page.pause();
        }

        // --- Guardar como borrador ---
        if (diag != null) diag.marca("pulso Save to draft");
        guardarBorrador(page);
        if (diag != null) diag.observar("después de guardar", 5);
    }

    /**
     * Pulsa "Save to draft" y confirma con la respuesta de MakerOnline (comprobado el 31/10/2026):
     * POST /api/mold/save-draft responde {code: 0, message: "Successful operation", data: {id, ...}}
     * y después la página salta a /en/user/uploads/draft.html.
     *
     * @throws IllegalStateException si no responde o no lo guarda
     */
    static void guardarBorrador(Page page, Progreso progreso) {
        progreso.paso("Guardando borrador");
        Locator boton = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("draft", Pattern.CASE_INSENSITIVE))).first();
        // La respuesta se lee en cuanto llega: justo después la web salta a otra página
        // y Chrome puede dejar de tenerla.
        AtomicReference<String> cuerpo = new AtomicReference<>();
        Response r;
        try {
            r = page.waitForResponse(resp -> {
                if (!resp.url().contains("/api/mold/save-draft")) return false;
                try {
                    cuerpo.set(resp.text());
                } catch (Exception e) {
                    // no se ha podido leer: se confirma con el salto de página (abajo)
                }
                return true;
            }, new Page.WaitForResponseOptions().setTimeout(60_000), boton::click);
        } catch (TimeoutError e) {
            throw new IllegalStateException("He pulsado 'Save to draft', pero MakerOnline no ha respondido en 60 s");
        }
        if (!r.ok()) {
            throw new IllegalStateException("MakerOnline no ha guardado el borrador (HTTP " + r.status() + ")");
        }

        JsonNode j = null;
        try {
            if (cuerpo.get() != null) j = JSON.readTree(cuerpo.get());
        } catch (Exception e) {
            // no es JSON: se confirma con el salto de página
        }
        if (j != null && j.path("code").asInt(-1) != 0) {
            throw new IllegalStateException("MakerOnline no ha guardado el borrador: "
                    + j.path("message").asText("código " + j.path("code").asText("?")));
        }

        // Cuando guarda bien, la web salta sola a la lista de borradores
        boolean salto;
        try {
            page.waitForURL("**/user/uploads/draft.html", new Page.WaitForURLOptions().setTimeout(15_000));
            salto = true;
        } catch (TimeoutError e) {
            salto = false;
        }
        if (j != null) {
            progreso.paso("MakerOnline confirma el borrador (id " + j.path("data").path("id").asText("?") + ")");
        } else if (salto) {
            progreso.paso("MakerOnline confirma el borrador (ha saltado a tus borradores)");
        } else {
            throw new IllegalStateException("No he podido confirmar que MakerOnline haya guardado el borrador."
                    + " Mira en tus borradores si está antes de volver a subirlo.");
        }
    }

    private void guardarBorrador(Page page) {
        guardarBorrador(page, progreso);
    }

    // ----------------------------------------------------------------- utilidades

    /** Parte cada entrada por comas ("a, b" → "a", "b"), quita espacios, vacíos y repetidos. */
    private static List<String> tagsLimpios(List<String> tags) {
        return tags.stream()
                .flatMap(t -> Stream.of(t.split(",")))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .distinct()
                .toList();
    }

    /** Zona de subida (.el-upload) que contiene el texto indicado. */
    private static Locator zonaSubida(Page page, String texto) {
        return page.locator(".el-upload").filter(new Locator.FilterOptions().setHasText(texto)).first();
    }

    /** Hace clic como una persona y responde al selector de archivos del navegador. */
    private static void elegirArchivo(Page page, Locator zona, Path archivo) {
        FileChooser fc = page.waitForFileChooser(zona::click);
        fc.setFiles(archivo);
    }

    private void paso(String texto) {
        progreso.paso(texto);
    }

    private static void captura(Page page, Path destino) {
        try {
            page.screenshot(new Page.ScreenshotOptions().setPath(destino).setFullPage(true));
        } catch (Exception ignored) {
            // si la página ya no responde, seguimos sin captura
        }
    }

    private void moverAErrores(Path dir, String mensaje) {
        try {
            Files.writeString(dir.resolve("error.txt"),
                    LocalDateTime.now().format(STAMP) + "\n" + mensaje + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Path destino = mover(dir, rutas.getErrores());
            progreso.info("   Movido a " + destino);
        } catch (IOException e) {
            progreso.error("No se pudo mover a errores/: " + e.getMessage());
        }
    }

    /**
     * Mueve la carpeta a destino con la fecha delante: 2026-09-23_2145_modelo-a
     *
     * En Windows no se puede mover una carpeta con archivos abiertos. Justo después de
     * cerrar la pestaña, Chrome puede tardar un momento en soltarlos: se reintenta unos segundos.
     */
    static Path mover(Path dir, Path carpetaDestino) throws IOException {
        Path destino = carpetaDestino.resolve(LocalDateTime.now().format(STAMP) + "_" + dir.getFileName());
        int n = 2;
        while (Files.exists(destino)) {
            destino = carpetaDestino.resolve(LocalDateTime.now().format(STAMP) + "_" + dir.getFileName() + "_" + n++);
        }
        for (int intento = 1; ; intento++) {
            try {
                return Files.move(dir, destino);
            } catch (AccessDeniedException e) {
                if (intento >= INTENTOS_MOVER) {
                    throw new IOException("la carpeta está en uso (¿la tienes abierta en el Explorador o en otro programa?): "
                            + dir, e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrumpido mientras se movía " + dir, ie);
                }
            }
        }
    }
}
