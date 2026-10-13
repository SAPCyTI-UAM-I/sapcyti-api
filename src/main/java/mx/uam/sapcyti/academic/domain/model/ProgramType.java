package mx.uam.sapcyti.academic.domain.model;

/**
 * Graduate program level for a student (HU-15, HU-62).
 * {@code ESPECIALIZACION} is stored as-is; existing {@code MAESTRIA} and {@code DOCTORADO} rows are not rewritten.
 */
public enum ProgramType {
    MAESTRIA,
    DOCTORADO,
    ESPECIALIZACION
}
