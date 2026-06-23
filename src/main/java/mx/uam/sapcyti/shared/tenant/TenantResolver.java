package mx.uam.sapcyti.shared.tenant;

import java.util.Optional;

import mx.uam.sapcyti.identity.domain.model.RoleType;

/**
 * Resolves the effective graduate program id from JWT claims and optional header.
 */
public final class TenantResolver {

    private TenantResolver() {
    }

    /**
     * @param role claim role name (e.g. COORDINATOR)
     * @param claimProgramId graduateProgramId from JWT (nullable)
     * @param headerProgramId parsed X-Graduate-Id (empty if absent)
     * @return program id to set in {@link TenantContext}, or empty when no tenant applies
     */
    public static Optional<Long> resolve(
            String role,
            Long claimProgramId,
            Optional<Long> headerProgramId) {
        if (RoleType.SYSTEM_ADMIN.name().equals(role)) {
            return headerProgramId;
        }
        if (claimProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        if (headerProgramId.isEmpty()) {
            return Optional.of(claimProgramId);
        }
        if (headerProgramId.get().equals(claimProgramId)) {
            return Optional.of(claimProgramId);
        }
        throw new TenantAccessDeniedException(TenantAccessDeniedException.MISMATCH_MESSAGE);
    }

    /**
     * Parses {@link TenantFilter#HEADER_GRADUATE_ID}; invalid values yield empty.
     */
    public static Optional<Long> parseGraduateHeader(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(rawHeader.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * For non-admin users, an unparseable header when present is treated as denial.
     */
    public static boolean isHeaderPresentButInvalid(String rawHeader) {
        return rawHeader != null && !rawHeader.isBlank() && parseGraduateHeader(rawHeader).isEmpty();
    }
}
