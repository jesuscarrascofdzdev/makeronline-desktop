package dev.jefe.makeronline;

import java.util.ArrayList;
import java.util.List;

/**
 * Los textos de cada modelo: salen del .json (info.json) o del .txt (ver TextosModelo).
 *
 * Obligatorios: title, description, tags, category.
 * Opcionales:   license   (por defecto "CC0 (aka CC Zero)" — el principio del texto de la opción de licencia)
 *               printType (por defecto "FDM")
 */
public record ModelMeta(
        String title,
        String description,
        List<String> tags,
        List<String> category,
        String license,
        String printType
) {
    public String licenseOrDefault()   { return blank(license)   ? "CC0 (aka CC Zero)" : license; }
    public String printTypeOrDefault() { return blank(printType) ? "FDM" : printType; }

    /** Devuelve la lista de problemas; vacía si todo está bien. */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        // Cada mensaje dice cómo se llama el dato en el .json y en el .txt
        if (blank(title)) errors.add("Falta el título (\"title\" en el .json, \"Título:\" en el .txt)");
        if (blank(description)) errors.add("Falta la descripción (\"description\" en el .json, \"Descripción:\" en el .txt)");
        if (tags == null || tags.isEmpty() || tags.stream().anyMatch(ModelMeta::blank))
            errors.add("Faltan las etiquetas: al menos una y ninguna vacía (\"tags\" en el .json, \"Etiquetas:\" en el .txt)");
        if (category == null || category.size() != 2 || category.stream().anyMatch(ModelMeta::blank))
            errors.add("La categoría tiene que tener 2 niveles: [\"Toys&Games\", \"Action Figures\"] en el .json, "
                    + "\"Categoría: Toys&Games > Action Figures\" en el .txt");
        return errors;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
