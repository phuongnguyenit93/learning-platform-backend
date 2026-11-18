package com.learningplatform.backend.module.core.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Shared validation for the required language query parameter. */
public final class ModuleApiRequestValidator {

    private ModuleApiRequestValidator() {
    }

    public static void validateLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.isBlank() || languageCode.length() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid language code");
        }
    }

    /** Reject malformed path segments before looking up public slugs. */
    public static void validateSlug(String slug, int maximumLength) {
        if (slug == null || slug.isBlank() || slug.length() > maximumLength
                || slug.chars().anyMatch(character ->
                        Character.isWhitespace(character) || Character.isISOControl(character)
                                || character == '/' || character == '\\' || character == '%')) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid slug");
        }
    }
}
