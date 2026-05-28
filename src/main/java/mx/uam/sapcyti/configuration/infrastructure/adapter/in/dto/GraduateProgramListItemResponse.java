package mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto;

/**
 * API response item for program listing (schema: GraduateProgramListResponse).
 */
public record GraduateProgramListItemResponse(
    Long id,
    String name,
    String division,
    long parameterCount) {
}
