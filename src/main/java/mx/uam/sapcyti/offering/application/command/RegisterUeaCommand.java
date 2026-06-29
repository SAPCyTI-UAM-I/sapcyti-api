package mx.uam.sapcyti.offering.application.command;

import java.math.BigDecimal;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;

public record RegisterUeaCommand(
        String clave,
        String nombre,
        UeaType tipo,
        UeaModality modalidad,
        BigDecimal horasTeoria,
        BigDecimal horasPractica,
        FormationType tipoFormacion,
        Integer creditos) {
}
