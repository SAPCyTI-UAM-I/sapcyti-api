package mx.uam.sapcyti.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables {@code @PreAuthorize} for RBAC. HTTP security is configured in
 * {@link mx.uam.sapcyti.identity.infrastructure.security.SecurityConfig} (SPEC-012).
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
