package co.edu.unipamplona.logicode.content.application;

import co.edu.unipamplona.logicode.shared.application.ApplicationException;

final class Texts {

    private Texts() {
    }

    static String required(String value, String message) {
        String cleaned = optional(value);
        if (cleaned == null) {
            throw ApplicationException.businessRule(message);
        }
        return cleaned;
    }

    static String optional(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.strip();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
