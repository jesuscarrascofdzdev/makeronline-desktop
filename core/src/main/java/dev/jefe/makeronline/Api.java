package dev.jefe.makeronline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

/**
 * Peticiones a la API de MakerOnline con tu sesión (sin cargar páginas).
 * La usan el Downloader y el buscador de usuarios (ver BuscadorUsuarios).
 *
 * @param req   el cliente de peticiones de Chrome (ctx.request())
 * @param token el valor de la cookie mo_access_token; la web lo manda como cabecera Authorization
 */
record Api(APIRequestContext req, String token) {

    static final String WEB = "https://www.makeronline.com";
    private static final ObjectMapper JSON = new ObjectMapper();

    JsonNode get(String path) {
        return leer(req.get(WEB + path, RequestOptions.create()
                .setHeader("Authorization", token)
                .setHeader("Accept", "application/json")), path);
    }

    JsonNode post(String path, Object cuerpo) {
        try {
            return leer(req.post(WEB + path, RequestOptions.create()
                    .setHeader("Authorization", token)
                    .setHeader("Accept", "application/json")
                    .setHeader("Content-Type", "application/json")
                    .setData(JSON.writeValueAsString(cuerpo))), path);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Devuelve el campo "data" de la respuesta; si MakerOnline responde con error, lanza una excepción. */
    private static JsonNode leer(APIResponse r, String path) {
        if (!r.ok()) throw new IllegalStateException("La API ha respondido HTTP " + r.status() + " a " + path);
        try {
            JsonNode j = JSON.readTree(r.text());
            if (j.path("code").asInt(-1) != 0) {
                throw new IllegalStateException("La API ha respondido '" + j.path("message").asText("?") + "' a " + path);
            }
            return j.path("data");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Respuesta no válida de " + path);
        }
    }
}
