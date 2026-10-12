package mx.uam.sapcyti.configuration.domain.exception;

/**
 * The compare-and-swap version is not the current mail-policy version.
 * The row and its audit history stay unchanged. A higher version is a conflict;
 * the store does not skip versions.
 */
public class MailPolicyVersionConflictException extends RuntimeException {

    public static final String MESSAGE = "Mail policy version conflict";

    private final Integer currentVersion;

    public MailPolicyVersionConflictException() {
        this(null);
    }

    /**
     * @param currentVersion version stored on the row; optional in the HTTP payload
     */
    public MailPolicyVersionConflictException(int currentVersion) {
        this(Integer.valueOf(currentVersion));
    }

    private MailPolicyVersionConflictException(Integer currentVersion) {
        super(MESSAGE);
        this.currentVersion = currentVersion;
    }

    /**
     * @return the stored version when the conflict was observed, or null if unknown
     */
    public Integer currentVersion() {
        return currentVersion;
    }
}
