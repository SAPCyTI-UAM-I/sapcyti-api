package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import mx.uam.sapcyti.academic.domain.model.DegreeLevel;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

/**
 * Request body for updating student personal and student-level academic fields (HU-18 / HU-56).
 */
public record UpdateStudentRequest(
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "First last name is required") String firstLastName,
        String secondLastName,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Nationality is required") String nationality,
        @NotNull(message = "Birth date is required") LocalDate birthDate,
        @NotBlank(message = "Phone is required") @Size(max = 20, message = "Phone must be at most 20 characters") String phone,
        @Size(max = 10, message = "Phone extension must be at most 10 characters") String phoneExtension,
        @NotBlank(message = "Undergraduate degree is required") String undergraduateDegree,
        @NotNull(message = "Last degree obtained is required") DegreeLevel lastDegreeObtained,
        @NotNull(message = "Program type is required") ProgramType programType,
        @NotNull(message = "Admission date is required") LocalDate admissionDate,
        // Optional (HU-56): blank allowed, format validated when present.
        @Pattern(regexp = "^(\\d{2}[OIPoip])?$", message = "Admission term must match AA + O|I|P")
                String admissionTerm,
        boolean active) {
}
