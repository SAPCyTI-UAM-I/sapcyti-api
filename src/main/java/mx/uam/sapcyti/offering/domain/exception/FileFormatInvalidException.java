package mx.uam.sapcyti.offering.domain.exception;

public class FileFormatInvalidException extends RuntimeException {

    public static final String ERROR_CODE = "FILE_FORMAT_INVALID";
    public static final String MESSAGE =
            "Formato de archivo inválido, utilice la plantilla proporcionada";

    public FileFormatInvalidException() {
        super(MESSAGE);
    }

    public FileFormatInvalidException(String message) {
        super(message);
    }
}
