package mx.uam.sapcyti.configuration.domain.exception;

/**
 * Thrown when a configuration parameter cannot be found for a program and key.
 */
public class ConfigurationParameterNotFoundException extends RuntimeException {

    public static final String MESSAGE = "Configuration parameter not found";

    public ConfigurationParameterNotFoundException() {
        super(MESSAGE);
    }

    public ConfigurationParameterNotFoundException(String key) {
        super(MESSAGE + " (key=" + key + ")");
    }
}
