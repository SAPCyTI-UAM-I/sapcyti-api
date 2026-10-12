package mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto;

import java.time.Instant;

import mx.uam.sapcyti.configuration.domain.model.MailMode;

/**
 * API response for GET and PUT /api/mail-policy (schema: MailPolicyResponse).
 * The four fields are always present, including null audit values.
 */
public record MailPolicyResponse(
        MailMode mode,
        int version,
        Instant updatedAt,
        Long updatedByUserId) {
}
