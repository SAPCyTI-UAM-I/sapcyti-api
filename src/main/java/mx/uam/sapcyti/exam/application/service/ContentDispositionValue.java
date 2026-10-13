package mx.uam.sapcyti.exam.application.service;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

/**
 * Builds one {@code Content-Disposition} value from the stored {@code originalName}.
 * The name is not taken from the request and does not change the bytes.
 */
public final class ContentDispositionValue {

    private static final String ATTR_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$&+-.^_`|~";

    private ContentDispositionValue() {
    }

    public static String format(String disposition, String originalName) {
        String sanitized = sanitize(originalName);
        String ascii = escapeQuoted(asciiFallback(sanitized));
        return disposition
                + "; filename=\""
                + ascii
                + "\"; filename*=UTF-8''"
                + rfc5987(sanitized);
    }

    static String sanitize(String originalName) {
        String source = originalName == null ? "" : originalName;
        StringBuilder kept = new StringBuilder(source.length());
        for (int index = 0; index < source.length(); index++) {
            char current = source.charAt(index);
            if (Character.isISOControl(current) || current == '\u2028' || current == '\u2029') {
                continue;
            }
            if (current == '/' || current == '\\' || current == '"' || current == '\''
                    || current == '\u201C' || current == '\u201D' || current == '\u2018' || current == '\u2019') {
                continue;
            }
            kept.append(current);
        }
        String withoutPath = stripParentReferences(kept.toString());
        if (withoutPath.isBlank()) {
            return "document.pdf";
        }
        return withoutPath;
    }

    private static String stripParentReferences(String value) {
        String current = value;
        while (current.contains("..")) {
            current = current.replace("..", "");
        }
        return current;
    }

    private static String asciiFallback(String sanitized) {
        String stripped = Normalizer.normalize(sanitized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        StringBuilder ascii = new StringBuilder(stripped.length());
        for (int index = 0; index < stripped.length(); index++) {
            char current = stripped.charAt(index);
            if (current >= 0x20 && current < 0x7F) {
                ascii.append(current);
            }
        }
        if (ascii.isEmpty()) {
            return "document.pdf";
        }
        return ascii.toString();
    }

    private static String escapeQuoted(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String rfc5987(String value) {
        StringBuilder encoded = new StringBuilder(value.length());
        for (byte unit : value.getBytes(StandardCharsets.UTF_8)) {
            int current = unit & 0xFF;
            if (current < 128 && ATTR_CHARS.indexOf(current) >= 0) {
                encoded.append((char) current);
            } else {
                encoded.append('%');
                encoded.append(Character.toUpperCase(Character.forDigit(current >>> 4, 16)));
                encoded.append(Character.toUpperCase(Character.forDigit(current & 0xF, 16)));
            }
        }
        return encoded.toString();
    }
}
