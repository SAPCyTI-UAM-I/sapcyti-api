package mx.uam.sapcyti.offering.infrastructure.adapter.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;

@Schema(name = "UeaCatalogItem", description = "UEA catalog read model (HU-39).")
public record UeaCatalogItemResponse(
        Long id,
        String clave,
        String nombre,
        UeaType tipo,
        UeaModality modalidad,
        double horasTeoria,
        double horasPractica,
        FormationType tipoFormacion,
        int creditos,
        boolean active) {
}
