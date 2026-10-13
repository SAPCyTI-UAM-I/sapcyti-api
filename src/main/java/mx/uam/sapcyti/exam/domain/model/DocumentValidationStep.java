package mx.uam.sapcyti.exam.domain.model;

/**
 * Steps applied to one document, in this order
 * (exam-requests.schema.json#/definitions/DocumentValidationStep).
 * The first failed step wins. Storage is not a step.
 */
public enum DocumentValidationStep {
    KIND_ADMISSION,
    SAFE_NAME,
    PDF_CONTENT,
    PRESCRIBED_NAME,
    ARTICLE_URL
}
