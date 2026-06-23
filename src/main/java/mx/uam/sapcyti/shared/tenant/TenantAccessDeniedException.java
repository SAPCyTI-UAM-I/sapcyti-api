package mx.uam.sapcyti.shared.tenant;

/**
 * Tenant header does not match JWT claim or user lacks program scope.
 */
public class TenantAccessDeniedException extends RuntimeException {

    public static final String MISMATCH_MESSAGE =
            "Graduate program id does not match authenticated context";
    public static final String MISSING_SCOPE_MESSAGE =
            "Authenticated user has no graduate program scope";
    public static final String INVALID_HEADER_MESSAGE = "Invalid graduate program id header";

    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
