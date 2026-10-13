package mx.uam.sapcyti.exam.domain.model;

/**
 * How a current document is stored (exam-requests.schema.json#/definitions/DocumentKind).
 * Only {@code ARTICULO} accepts {@code URL}. Every other GRADO type accepts PDF only.
 */
public enum DocumentKind {
    PDF,
    URL
}
