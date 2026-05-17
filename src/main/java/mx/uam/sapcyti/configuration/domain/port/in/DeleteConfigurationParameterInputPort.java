package mx.uam.sapcyti.configuration.domain.port.in;

/**
 * Input port for deleting a configuration parameter.
 */
public interface DeleteConfigurationParameterInputPort {

    void delete(Long graduateProgramId, String key);
}
