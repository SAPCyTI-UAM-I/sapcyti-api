package mx.uam.sapcyti.exam.domain.model;

import java.util.regex.Pattern;

/**
 * Lowercase SHA-256 hex required by the exam-request schema.
 */
final class Sha256Hashes {

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private Sha256Hashes() {
    }

    static String require(String value) {
        if (value == null || !SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException("sha256 must be 64 lowercase hex characters");
        }
        return value;
    }
}
