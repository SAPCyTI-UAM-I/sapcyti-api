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
import mx.uam.sapcyti.academic.domain.model.ProfessorType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfessorRequest {

    @NotNull(message = "Professor type is required")
    private ProfessorType professorType;

    @Size(max = 20, message = "Employee number must be at most 20 characters")
    private String employeeNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "First last name is required")
    private String firstLastName;

    private String secondLastName;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must be at most 20 characters")
    private String phone;

    @Size(max = 10, message = "Phone extension must be at most 10 characters")
    private String phoneExtension;

    @NotNull(message = "Commission member flag is required")
    private Boolean commissionMember;

    private LocalDate nextSabbaticalStart;

    private LocalDate nextSabbaticalEnd;
}
