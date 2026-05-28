package mx.uam.sapcyti.configuration.infrastructure.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import mx.uam.sapcyti.configuration.application.command.SetConfigurationParameterCommand;
import mx.uam.sapcyti.configuration.domain.exception.ConfigurationParameterNotFoundException;
import mx.uam.sapcyti.configuration.domain.exception.GraduateProgramNotFoundException;
import mx.uam.sapcyti.configuration.domain.port.in.DeleteConfigurationParameterInputPort;
import mx.uam.sapcyti.configuration.domain.port.in.GetConfigurationParametersInputPort;
import mx.uam.sapcyti.configuration.domain.port.in.SetConfigurationParameterInputPort;
import mx.uam.sapcyti.configuration.infrastructure.adapter.in.dto.ConfigurationParameterResponse;
import mx.uam.sapcyti.configuration.infrastructure.mapper.ConfigurationParameterMapper;
import mx.uam.sapcyti.shared.config.MethodSecurityConfig;
import mx.uam.sapcyti.shared.web.GlobalExceptionHandler;

@ActiveProfiles("test")
@WebMvcTest(ConfigurationParameterController.class)
@Import({MethodSecurityConfig.class, GlobalExceptionHandler.class})
class ConfigurationParameterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SetConfigurationParameterInputPort setPort;

    @MockitoBean
    private GetConfigurationParametersInputPort getPort;

    @MockitoBean
    private DeleteConfigurationParameterInputPort deletePort;

    @MockitoBean
    private ConfigurationParameterMapper mapper;

    @Test
    @DisplayName("Scenario: Set a new configuration parameter")
    void shouldSetParameter() throws Exception {
        SetConfigurationParameterCommand command = new SetConfigurationParameterCommand(
            "MAX_COURSES_PER_TERM", "3", "Maximum UEAs per term");
        ConfigurationParameterResponse response = new ConfigurationParameterResponse(
            command.key(), command.value(), command.description());

        when(mapper.toSetCommand(any())).thenReturn(command);
        when(setPort.set(eq(1L), eq(command))).thenReturn(response);

        mockMvc.perform(post("/api/programs/1/parameters")
                .with(user("coordinator").roles("COORDINATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "key": "MAX_COURSES_PER_TERM",
                      "value": "3",
                      "description": "Maximum UEAs per term"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("MAX_COURSES_PER_TERM"));
    }

    @Test
    @DisplayName("Scenario: Rejection of parameter with invalid key format")
    void shouldRejectInvalidKeyFormat() throws Exception {
        mockMvc.perform(post("/api/programs/1/parameters")
                .with(user("coordinator").roles("COORDINATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"key": "max-courses", "value": "3"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message")
                .value("Key must be in UPPER_SNAKE_CASE format"));
    }

    @Test
    @DisplayName("Scenario: Rejection of parameter with empty value")
    void shouldRejectEmptyValue() throws Exception {
        mockMvc.perform(post("/api/programs/1/parameters")
                .with(user("coordinator").roles("COORDINATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"key": "MAX_COURSES_PER_TERM", "value": ""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Parameter value is required"));
    }

    @Test
    @DisplayName("Scenario: Rejection of operation on non-existent program")
    void shouldReturn404ForMissingProgram() throws Exception {
        SetConfigurationParameterCommand command = new SetConfigurationParameterCommand(
            "MAX_COURSES_PER_TERM", "3", null);

        when(mapper.toSetCommand(any())).thenReturn(command);
        when(setPort.set(eq(999L), eq(command)))
            .thenThrow(new GraduateProgramNotFoundException(999L));

        mockMvc.perform(post("/api/programs/999/parameters")
                .with(user("coordinator").roles("COORDINATOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"key": "MAX_COURSES_PER_TERM", "value": "3"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Graduate program not found"));
    }

    @Test
    @DisplayName("Scenario: Deletion of non-existent parameter")
    void shouldReturn404OnDeleteMissingParameter() throws Exception {
        doThrow(new ConfigurationParameterNotFoundException("UNKNOWN"))
            .when(deletePort).delete(1L, "UNKNOWN");

        mockMvc.perform(delete("/api/programs/1/parameters/UNKNOWN")
                .with(user("coordinator").roles("COORDINATOR")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message")
                .value("Configuration parameter not found"));
    }

    @Test
    @DisplayName("Scenario: Rejection of access with unauthorized role")
    void shouldDenyStudentRole() throws Exception {
        mockMvc.perform(get("/api/programs/1/parameters")
                .with(user("student").roles("STUDENT")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario: List parameters for a program")
    void shouldListParameters() throws Exception {
        when(getPort.listByProgram(1L)).thenReturn(List.of(
            new ConfigurationParameterResponse(
                "MAX_COURSES_PER_TERM", "3", "Maximum UEAs per term")));

        mockMvc.perform(get("/api/programs/1/parameters")
                .with(user("coordinator").roles("COORDINATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].key").value("MAX_COURSES_PER_TERM"));
    }

    @Test
    @DisplayName("should delete parameter and return 204")
    void shouldDeleteParameter() throws Exception {
        mockMvc.perform(delete("/api/programs/1/parameters/MAX_COURSES_PER_TERM")
                .with(user("coordinator").roles("COORDINATOR")))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Scenario: Get parameter by key")
    void shouldGetParameterByKey() throws Exception {
        when(getPort.getByProgramAndKey(1L, "MAX_COURSES_PER_TERM"))
            .thenReturn(Optional.of(new ConfigurationParameterResponse(
                "MAX_COURSES_PER_TERM", "3", null)));

        mockMvc.perform(get("/api/programs/1/parameters/MAX_COURSES_PER_TERM")
                .with(user("coordinator").roles("COORDINATOR")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value("3"));
    }
}
