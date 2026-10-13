package mx.uam.sapcyti.exam.domain.model;

/**
 * Reason published when a read is allowed and the student is not eligible
 * (exam-requests.schema.json#/definitions/EligibilityReason). Null when eligible.
 */
public enum EligibilityReason {
    PROGRAM_BAJA,
    PROGRAM_SUSPENSION
}
