package mx.uam.sapcyti.identity.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({PasswordResetProperties.class, ResendProperties.class})
public class IdentityMailConfig {

    @Bean
    @ConditionalOnProperty(name = "app.email.provider", havingValue = "resend")
    RestClient resendRestClient(ResendProperties resendProperties) {
        if (!StringUtils.hasText(resendProperties.apiKey())) {
            throw new IllegalStateException(
                    "RESEND_API_KEY is required when app.email.provider=resend");
        }
        return RestClient.builder()
                .baseUrl(resendProperties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + resendProperties.apiKey())
                .build();
    }
}
