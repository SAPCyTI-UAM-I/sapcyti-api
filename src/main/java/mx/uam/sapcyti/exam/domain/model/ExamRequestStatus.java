package mx.uam.sapcyti.exam.domain.model;

/**
 * Status of an existing request summary (exam-requests.schema.json#/definitions/ExamRequestStatus).
 * HU-62 only reads it.
 */
public enum ExamRequestStatus {
    BORRADOR,
    ENVIADA,
    ACTUALIZADA,
    CERRADA
}
