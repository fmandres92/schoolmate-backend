package com.schoolmate.api.common.rut;

import java.util.Locale;

public final class RutNormalizer {

    private RutNormalizer() {
    }

    public static String normalize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("RUT inválido");
        }

        String cleaned = input.trim()
            .replace(".", "")
            .replace(" ", "")
            .replace("-", "")
            .toUpperCase(Locale.ROOT);

        if (cleaned.length() < 2) {
            throw new IllegalArgumentException("RUT inválido");
        }

        String body = cleaned.substring(0, cleaned.length() - 1);
        String dv = cleaned.substring(cleaned.length() - 1);
        return body + "-" + dv;
    }
}
