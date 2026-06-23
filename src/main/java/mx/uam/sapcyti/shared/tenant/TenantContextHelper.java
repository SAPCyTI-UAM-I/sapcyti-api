package mx.uam.sapcyti.shared.tenant;

import java.util.Optional;

import org.slf4j.MDC;

/**
 * Applies resolved tenant to {@link TenantContext} and logging MDC.
 */
public final class TenantContextHelper {

    private TenantContextHelper() {
    }

    public static void apply(Optional<Long> programId, String userId) {
        programId.ifPresent(id -> {
            TenantContext.set(id);
            MDC.put(TenantFilter.MDC_GRADUATE_PROGRAM_ID, String.valueOf(id));
        });
        if (userId != null && !userId.isBlank()) {
            MDC.put(TenantFilter.MDC_USER_ID, userId);
        }
    }
}
