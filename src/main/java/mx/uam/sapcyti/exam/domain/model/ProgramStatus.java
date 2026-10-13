package mx.uam.sapcyti.exam.domain.model;

/**
 * Status copied from the same academic row as the degree
 * (exam-requests.schema.json#/definitions/ProgramStatus).
 */
public enum ProgramStatus {
    EN_INVESTIGACION,
    EGRESADO,
    BAJA,
    SUSPENSION
}
