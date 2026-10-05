package mx.uam.sapcyti.offering.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;

public record UpdateUeaRequest(
        @NotBlank(message = "nombre is required")
        @Size(max = 200, message = "nombre must be at most 200 characters")
        String nombre,
        @NotNull(message = "tipo is required")
        UeaType tipo,
        @NotNull(message = "modalidad is required")
        UeaModality modalidad,
        @NotNull(message = "horasTeoria is required")
        @DecimalMin(value = "0", message = "horasTeoria must be greater than or equal to 0")
        BigDecimal horasTeoria,
        @NotNull(message = "horasPractica is required")
        @DecimalMin(value = "0", message = "horasPractica must be greater than or equal to 0")
        BigDecimal horasPractica,
        @NotNull(message = "tipoFormacion is required")
        FormationType tipoFormacion,
        @NotNull(message = "creditos is required")
        @Positive(message = "creditos must be greater than 0")
        Integer creditos) {
}
