package mx.uam.sapcyti.exam.domain.model;

/**
 * Kind of exam request (exam-requests.schema.json#/definitions/ExamType).
 * {@code PREDOCTORAL} is not a program type. Its requirement catalog is HU-63.
 */
public enum ExamType {
    GRADO,
    PREDOCTORAL
}
