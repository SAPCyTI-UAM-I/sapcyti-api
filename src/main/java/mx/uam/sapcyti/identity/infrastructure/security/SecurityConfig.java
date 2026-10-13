package mx.uam.sapcyti.identity.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@Profile("!test")
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean prod = environment.matchesProfiles("prod");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                                    "/api/auth/forgot-password", "/api/auth/reset-password")
                            .permitAll();
                    // Actuator: permitAll so non-exposed endpoints return 404 (prod exposes health only).
                    // Docs: permitAll only outside prod (springdoc disabled + no public matchers in prod).
                    if (prod) {
                        auth.requestMatchers("/actuator/**").permitAll();
                    } else {
                        auth.requestMatchers("/actuator/**", "/api-docs/**", "/docs/**", "/swagger-ui/**")
                                .permitAll();
                    }
                    // SPEC-041: role is decided before MVC binds the mail-policy body.
                    auth.requestMatchers(HttpMethod.GET, "/api/mail-policy").hasRole("COORDINATOR");
                    auth.requestMatchers(HttpMethod.PUT, "/api/mail-policy").hasRole("COORDINATOR");
                    // SPEC-047: STUDENT is decided before MVC binds these reads. Do not open the prefix.
                    auth.requestMatchers(HttpMethod.GET, "/api/exam-requests/me/options").hasRole("STUDENT");
                    auth.requestMatchers(HttpMethod.GET, "/api/exam-requests/me/*/requirements")
                            .hasRole("STUDENT");
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
