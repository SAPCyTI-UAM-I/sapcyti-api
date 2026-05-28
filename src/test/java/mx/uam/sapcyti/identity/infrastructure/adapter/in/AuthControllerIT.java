package mx.uam.sapcyti.identity.infrastructure.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import mx.uam.sapcyti.identity.application.service.JwtService;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.identity.domain.port.in.AuthInputPort;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.AuthResponse;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for this test
@ActiveProfiles("test")
class AuthControllerIT {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthInputPort authInputPort;
        @MockitoBean
        private JwtService jwtService;

        @Test
        @DisplayName("should return 200 and set cookie on successful login")
        void shouldReturn200OnLogin() throws Exception {
                LoginRequest request = LoginRequest.builder()
                                .email("test@uam.mx")
                                .password("password")
                                .build();

                AuthInputPort.LoginResult result = AuthInputPort.LoginResult.builder()
                                .authResponse(AuthResponse.builder()
                                                .accessToken("access")
                                                .expiresIn(900)
                                                .role(RoleType.STUDENT)
                                                .build())
                                .refreshToken("refresh")
                                .refreshExpiresIn(604800)
                                .build();

                when(authInputPort.login(any())).thenReturn(result);

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access"))
                                .andExpect(header().exists("Set-Cookie"))
                                .andExpect(header().string("Set-Cookie",
                                                org.hamcrest.Matchers.containsString("refreshToken=refresh")));
        }
}
