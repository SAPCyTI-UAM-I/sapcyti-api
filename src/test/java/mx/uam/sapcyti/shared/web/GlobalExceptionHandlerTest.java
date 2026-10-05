package mx.uam.sapcyti.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import mx.uam.sapcyti.academic.domain.exception.EmployeeNumberImmutableException;
import mx.uam.sapcyti.academic.domain.exception.InvalidTypeChangeException;
import mx.uam.sapcyti.academic.domain.exception.ProfessorAlreadyActiveException;
import mx.uam.sapcyti.configuration.domain.exception.DuplicateGraduateProgramNameException;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldMapProgramNotFoundTo404() {
        ResponseEntity<ErrorResponse> response = handler.handleProgramNotFound(
            new GraduateProgramNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message())
            .isEqualTo("Graduate program not found");
    }

    @Test
    void shouldMapNoResourceFoundTo404() {
        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(
            new NoResourceFoundException(HttpMethod.GET, "actuator/info"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
    }

    @Test
    void shouldMapDuplicateNameTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateName(
            new DuplicateGraduateProgramNameException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("CONFLICT");
        assertThat(response.getBody().message())
            .isEqualTo("A graduate program with that name already exists");
    }

    @Test
    void shouldMapIllegalArgumentTo400() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
            new IllegalArgumentException("Parameter key is required"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error()).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void shouldMapEmployeeNumberImmutableTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleEmployeeNumberImmutable(
            new EmployeeNumberImmutableException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("NEMP_IMMUTABLE");
        assertThat(response.getBody().message())
            .isEqualTo(EmployeeNumberImmutableException.MESSAGE);
    }

    @Test
    void shouldMapInvalidTypeChangeTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidTypeChange(
            new InvalidTypeChangeException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("INVALID_TYPE_CHANGE");
        assertThat(response.getBody().message())
            .isEqualTo(InvalidTypeChangeException.MESSAGE);
    }

    @Test
    void shouldMapProfessorAlreadyActiveTo409() {
        ResponseEntity<ErrorResponse> response = handler.handleProfessorAlreadyActive(
            new ProfessorAlreadyActiveException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("PROFESSOR_ALREADY_ACTIVE");
        assertThat(response.getBody().message())
            .isEqualTo(ProfessorAlreadyActiveException.MESSAGE);
    }
}
