package mx.uam.sapcyti.survey.application.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UeaDemandRow {
    Long ueaId;
    String clave;
    String nombre;
    String tipoFormacion;
    int creditos;
    long totalResponses;
}
