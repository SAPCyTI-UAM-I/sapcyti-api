package mx.uam.sapcyti.configuration.application.command;

import java.util.List;

/**
 * Command to create a graduate program (schema: CreateGraduateProgramCommand).
 */
public record CreateGraduateProgramCommand(
    String name,
    String division,
    List<InitialParameterCommand> initialParameters) {

    public CreateGraduateProgramCommand(String name, String division) {
        this(name, division, List.of());
    }
}
