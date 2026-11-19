package com.learningplatform.backend.shared.persistence;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Validation shared by admin catalog writes; public GET APIs are unchanged. */
public final class CatalogInput {
    private static final Pattern CODE = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private CatalogInput() { }

    public static String code(String input) {
        String value = required(input, "CODE", 50).toUpperCase(Locale.ROOT);
        if (!CODE.matcher(value).matches()) {
            throw badRequest("CODE must use letters, digits or underscores and start with a letter");
        }
        return value;
    }

    public static String name(String input) {
        return required(input, "NAME", 150);
    }

    public static String slug(String input) {
        String value = required(input, "SLUG", 100).toLowerCase(Locale.ROOT);
        if (!SLUG.matcher(value).matches()) {
            throw badRequest("SLUG must use lowercase letters, numbers and hyphens");
        }
        return value;
    }

    public static String description(String input) {
        if (input == null || input.isBlank()) return null;
        String value = input.trim();
        if (value.length() > 500) throw badRequest("DESCRIPTION must be at most 500 characters");
        return value;
    }

    private static String required(String input, String field, int maxLength) {
        if (input == null || input.isBlank()) throw badRequest(field + " is required");
        String value = input.trim();
        if (value.length() > maxLength) throw badRequest(field + " must be at most " + maxLength + " characters");
        return value;
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
