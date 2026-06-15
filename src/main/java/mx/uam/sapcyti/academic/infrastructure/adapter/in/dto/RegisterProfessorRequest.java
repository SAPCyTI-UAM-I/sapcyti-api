package mx.uam.sapcyti.academic.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterProfessorRequest {

    @NotBlank(message = "Employee number is required")
    @Size(max = 20, message = "Employee number must be at most 20 characters")
    private String employeeNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Graduate program id is required")
    private Long graduateProgramId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "First last name is required")
    private String firstLastName;

    private String secondLastName;
}
