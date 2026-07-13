package mx.uam.sapcyti.survey.domain.exception;

public class UeaNotAvailableException extends RuntimeException {

    public static final String ERROR_CODE = "UEA_NOT_AVAILABLE";
    public static final String MESSAGE =
            "Una o más UEA seleccionadas ya no están disponibles. Actualiza tu selección.";

    public UeaNotAvailableException() {
        super(MESSAGE);
    }
}
