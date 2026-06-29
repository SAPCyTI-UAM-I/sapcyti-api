package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.ProgramType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterStudentRequest {

    @NotBlank(message = "Enrollment id is required")
    @Size(max = 20, message = "Enrollment id must be at most 20 characters")
    private String enrollmentId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Graduate program id is required")
    private Long graduateProgramId;

    private Long advisorId;

    private java.util.List<Long> advisorIds;

    private Long tutorId;

    private String lineOfKnowledge;

    private String researchArea;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "First last name is required")
    private String firstLastName;

    private String secondLastName;

    @NotBlank(message = "Nationality is required")
    private String nationality;

    @NotNull(message = "Birth date is required")
    private LocalDate birthDate;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Size(max = 10, message = "Phone extension must be at most 10 characters")
    private String phoneExtension;

    @NotBlank(message = "Undergraduate degree is required")
    private String undergraduateDegree;

    @NotBlank(message = "Last degree obtained is required")
    private String lastDegreeObtained;

    @NotNull(message = "Program type is required")
    private ProgramType programType;

    @NotNull(message = "Admission date is required")
    private LocalDate admissionDate;
}
