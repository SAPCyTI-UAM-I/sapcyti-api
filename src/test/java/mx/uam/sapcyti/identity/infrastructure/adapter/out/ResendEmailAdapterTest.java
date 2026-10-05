package mx.uam.sapcyti.identity.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import mx.uam.sapcyti.identity.infrastructure.config.PasswordResetProperties;
import mx.uam.sapcyti.identity.infrastructure.config.ResendProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

class ResendEmailAdapterTest {

    private RestClient.RequestBodyUriSpec uriSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;
    private RestClient resendRestClient;
    private SpringTemplateEngine templateEngine;
    private ResendEmailAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        resendRestClient = mock(RestClient.class);
        uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
        templateEngine = mock(SpringTemplateEngine.class);

        when(resendRestClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri("/emails")).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.body(any(Map.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());
        when(templateEngine.process(eq("email/password-reset_es"), any(Context.class)))
                .thenReturn("<p>reset</p>");
        when(templateEngine.process(eq("email/password-reset_en"), any(Context.class)))
                .thenReturn("<p>reset</p>");

        adapter = new ResendEmailAdapter(
                resendRestClient,
                templateEngine,
                new PasswordResetProperties("https://spa.example.com", 30),
                new ResendProperties("re_test_key", "SAPCyTI <soporte@sapcyti.site>", "https://api.resend.com")
        );
    }

    @Test
    @DisplayName("sendPasswordReset posts HTML email to Resend /emails")
    @SuppressWarnings("unchecked")
    void sendPasswordResetPostsToResend() {
        adapter.sendPasswordReset("alumno@uam.mx", "raw-token", Locale.forLanguageTag("es"));

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(bodySpec).body(bodyCaptor.capture());

        Map<String, Object> body = bodyCaptor.getValue();
        assertThat(body.get("from")).isEqualTo("SAPCyTI <soporte@sapcyti.site>");
        assertThat(body.get("to")).asList().containsExactly("alumno@uam.mx");
        assertThat(body.get("subject")).isEqualTo("SAPCyTI — Recuperación de contraseña");
        assertThat(body.get("html")).isEqualTo("<p>reset</p>");

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/password-reset_es"), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getVariable("resetUrl"))
                .isEqualTo("https://spa.example.com/auth/reset-password?token=raw-token");
    }

    @Test
    @DisplayName("sendPasswordReset wraps Resend HTTP errors")
    void sendPasswordResetWrapsHttpErrors() {
        when(responseSpec.toBodilessEntity()).thenThrow(new RestClientResponseException(
                "Bad Request", HttpStatusCode.valueOf(400), "Bad Request", null, null, null));

        assertThatThrownBy(() -> adapter.sendPasswordReset("alumno@uam.mx", "tok", Locale.ENGLISH))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Resend");
    }
}
