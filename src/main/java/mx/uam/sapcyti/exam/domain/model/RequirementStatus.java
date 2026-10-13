package mx.uam.sapcyti.exam.domain.model;

/**
 * {@code CARGADO} when the request has a current document of this type;
 * otherwise {@code PENDIENTE} (exam-requests.schema.json#/definitions/RequirementStatus).
 */
public enum RequirementStatus {
    PENDIENTE,
    CARGADO
}
