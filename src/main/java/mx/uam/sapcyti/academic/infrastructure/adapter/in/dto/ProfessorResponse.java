package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Read model for professor data (HU-21).
 */
public record ProfessorResponse(
        Long id,
        String employeeNumber,
        String email,
        String firstName,
        String firstLastName,
        String secondLastName,
        String phone,
        String phoneExtension,
        boolean commissionMember,
        LocalDate nextSabbaticalStart,
        LocalDate nextSabbaticalEnd,
        Long graduateProgramId,
        @Schema(description = "Identifier of the user account linked to this professor.", accessMode = Schema.AccessMode.READ_ONLY)
        Long userId,
        @Schema(description = "Whether the professor account is active.", accessMode = Schema.AccessMode.READ_ONLY)
        boolean active,
        @Schema(
                description = "One-time server-generated temporary password. Present only in the registration (create) "
                        + "response; null on list/get.",
                accessMode = Schema.AccessMode.READ_ONLY)
        String generatedPassword) {
}
