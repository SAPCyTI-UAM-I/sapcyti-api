package mx.uam.sapcyti.survey.infrastructure.adapter.in.dto;

public record UeaDemandRowResponse(
        Long ueaId,
        String clave,
        String nombre,
        String tipoFormacion,
        int creditos,
        long totalResponses) {}
