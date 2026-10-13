package mx.uam.sapcyti.exam.application.exception;

/**
 * A doctorate asked for {@code PREDOCTORAL} requirements.
 *
 * <p>HU-63 publishes that 200 on the same route. This cut does not fill the response with the
 * {@code GRADO} catalog and does not assign {@code EXAM_TYPE_NOT_ALLOWED}.
 */
public class PredoctoralCatalogNotInThisCutException extends RuntimeException {

    public PredoctoralCatalogNotInThisCutException() {
        super("Predoctoral requirements are not published in this cut");
    }
}
