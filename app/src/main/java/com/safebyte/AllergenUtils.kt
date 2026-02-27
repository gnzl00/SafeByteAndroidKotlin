package com.safebyte

import java.text.Normalizer

val defaultAllergenCatalog = listOf(
    "Gluten",
    "Lacteos",
    "Huevo",
    "Frutos secos",
    "Mariscos",
    "Soja"
)

fun canonicalizeAllergen(raw: String): String? {
    val value = raw.trim()
    if (value.isEmpty()) {
        return null
    }

    val token = normalizeAllergenToken(value)
    return when {
        token.contains("gluten") -> "Gluten"
        token.contains("lacte") -> "Lacteos"
        token.contains("huevo") -> "Huevo"
        token.contains("fruto") && token.contains("seco") -> "Frutos secos"
        token.contains("marisc") -> "Mariscos"
        token.contains("soja") || token.contains("soy") -> "Soja"
        token.contains("pescad") -> "Pescado"
        token.contains("sesam") -> "Sesamo"
        else -> value
    }
}

fun allergenKey(raw: String): String {
    val canonical = canonicalizeAllergen(raw)?.trim().orEmpty()
    return normalizeAllergenToken(canonical)
}

fun normalizeAllergenSet(values: Iterable<String>): Set<String> {
    val seen = linkedSetOf<String>()
    val normalized = linkedSetOf<String>()
    values.forEach { raw ->
        val canonical = canonicalizeAllergen(raw)?.trim().orEmpty()
        if (canonical.isEmpty()) {
            return@forEach
        }
        val key = allergenKey(canonical)
        if (key.isEmpty() || !seen.add(key)) {
            return@forEach
        }
        normalized.add(canonical)
    }
    return normalized
}

fun hasAllergenConflict(itemAllergens: Iterable<String>, userAllergens: Set<String>): Boolean {
    val userKeys = userAllergens.map { allergenKey(it) }.toSet()
    return itemAllergens.any { allergenKey(it) in userKeys }
}

private fun normalizeAllergenToken(raw: String): String {
    val fixed = raw.trim()
        .replace("Ã¡", "a")
        .replace("Ã©", "e")
        .replace("Ã­", "i")
        .replace("Ã³", "o")
        .replace("Ãº", "u")
        .replace("Ã±", "n")
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ú", "u")
        .replace("ñ", "n")
    val noAccents = Normalizer.normalize(fixed.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
    return noAccents
        .replace("[^a-z0-9\\s]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ")
        .trim()
}
