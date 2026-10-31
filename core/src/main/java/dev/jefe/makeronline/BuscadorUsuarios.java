package dev.jefe.makeronline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encuentra en MakerOnline a un usuario de la lista (usuarios.json). La entrada puede ser:
 * <ul>
 *   <li>la URL de su perfil o su id, con o sin un nombre delante ("Ricardo · https://…/&lt;id&gt;.html"):
 *       lo más fiable, porque no depende del buscador;</li>
 *   <li>"@usuario_1234567 / NombreVisible";</li>
 *   <li>solo el @ o solo el nombre visible.</li>
 * </ul>
 * Lo usan el Downloader (al repasar la lista) y la app (al añadir a alguien a la lista).
 * Si no lo encuentra, lo cuenta con {@code progreso.error(...)} y devuelve null.
 */
public final class BuscadorUsuarios {

    private static final String WEB = Api.WEB;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern UUID_RE = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /**
     * Un usuario encontrado.
     *
     * @param id     su id de MakerOnline (el de la URL del perfil)
     * @param nombre su nombre visible
     * @param handle su @ ("@usuario_1234567"), o vacío si no se sabe
     */
    public record Usuario(String id, String nombre, String handle) {
        /** La URL de su perfil: es lo más fiable para apuntarlo en la lista. */
        public String url() {
            return WEB + "/en/user/personalInfo/" + id + ".html";
        }
    }

    private final Page page;
    private final Api api;
    private final Progreso progreso;

    BuscadorUsuarios(Page page, Api api, Progreso progreso) {
        this.page = page;
        this.api = api;
        this.progreso = progreso;
    }

    /** Busca al usuario de esta entrada; null si no se encuentra (el motivo va a progreso.error). */
    public Usuario buscar(String entrada) throws JsonProcessingException {
        // URL del perfil o id directamente: el nombre se saca de la página del perfil
        Matcher mm = UUID_RE.matcher(entrada);
        if (mm.find()) {
            String id = mm.group().toLowerCase();
            page.navigate(WEB + "/en/user/personalInfo/" + id + ".html");
            Object s = page.evaluate("() => { const d = window.__NUXT__ && window.__NUXT__.data && window.__NUXT__.data[0];"
                    + " return d && d.userInfo && d.userInfo.id ? JSON.stringify(d.userInfo) : null; }");
            if (s == null) {
                progreso.error("No encuentro ningún usuario con id " + id);
                return null;
            }
            return usuario(JSON.readTree((String) s));
        }

        // El buscador de MakerOnline busca por el NOMBRE VISIBLE, no por el @. El @ conserva el
        // nombre de cuenta original (p. ej. @User_1234567 puede llamarse "Pepe"), así que con solo el @
        // a veces no aparece. Por eso se admite "@handle / nombre visible": se busca por el
        // nombre y se elige la cuenta cuyo @ coincide exactamente.
        String clave;              // @ sin la arroba, o el nombre si no hay @
        boolean soloPorHandle;     // con "@handle / nombre" solo vale coincidir el @
        Set<String> busquedas = new LinkedHashSet<>();
        String[] partes = entrada.split("\\s*/\\s*");
        if (partes.length == 2 && !partes[0].isBlank() && !partes[1].isBlank()) {
            boolean primeroEsHandle = partes[0].startsWith("@") || !partes[1].startsWith("@");
            String handle = primeroEsHandle ? partes[0] : partes[1];
            String nombre = primeroEsHandle ? partes[1] : partes[0];
            clave = handle.replaceFirst("^@", "");
            soloPorHandle = true;
            busquedas.add(nombre);
        } else if (partes.length > 1) {
            progreso.error("No entiendo '" + entrada + "'. Usa \"@usuario_1234567 / NombreVisible\", el @, el nombre o la URL.");
            return null;
        } else {
            // "NombreVisible", "@Nombre" o "@NombreUsuario_1234567"
            clave = entrada.replaceFirst("^@", "");
            soloPorHandle = false;
        }
        busquedas.add(clave);
        busquedas.add(clave.replaceFirst("_\\d+$", ""));

        List<JsonNode> vistos = new ArrayList<>();
        for (String keyword : busquedas) {
            JsonNode data = api.post("/api/search/user", Map.of("keyword", keyword, "page", 1, "page_size", 20));
            List<JsonNode> porHandle = new ArrayList<>();
            List<JsonNode> porNombre = new ArrayList<>();
            for (JsonNode n : data.path("data")) {
                vistos.add(n);
                if (n.path("show_user_name").asText("").equalsIgnoreCase("@" + clave)) porHandle.add(n);
                else if (!soloPorHandle && (n.path("name").asText("").equalsIgnoreCase(clave)
                        || n.path("display_name").asText("").equalsIgnoreCase(clave))) porNombre.add(n);
            }
            if (porHandle.size() == 1) return usuario(porHandle.get(0));
            if (porNombre.size() == 1) return usuario(porNombre.get(0));
            if (porNombre.size() > 1) {
                StringBuilder msg = new StringBuilder("Hay varios usuarios llamados '" + clave
                        + "'. Usa su @ completo o la URL del perfil:");
                porNombre.forEach(n -> msg.append("\n   ").append(describir(n)));
                progreso.error(msg.toString());
                return null;
            }
        }
        StringBuilder msg = new StringBuilder("No encuentro al usuario '" + entrada + "'.\n");
        msg.append(soloPorHandle
                ? "  Revisa que el nombre visible esté bien escrito, o pon la URL de su perfil."
                : "  Si su nombre visible es distinto del @, escribe \"@usuario_1234567 / NombreVisible\" o pon la URL de su perfil.");
        if (!vistos.isEmpty()) {
            msg.append("\n  Parecidos:");
            vistos.stream().limit(5).forEach(n -> msg.append("\n   ").append(describir(n)));
        }
        progreso.error(msg.toString());
        return null;
    }

    /** "@usuario_1234567 / NombreVisible  (12 modelos)" de un resultado del buscador. */
    private static String describir(JsonNode n) {
        return n.path("show_user_name").asText() + " / " + n.path("display_name").asText()
                + "  (" + n.path("designs").asInt() + " modelos)";
    }

    private static Usuario usuario(JsonNode n) {
        String id = n.path("user_id").asText(n.path("id").asText());
        String nombre = n.path("display_name").asText(n.path("name").asText(id));
        return new Usuario(id, nombre, n.path("show_user_name").asText(""));
    }
}
