package mx.uam.sapcyti.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stack-smoke security for the {@code docker} profile only (SPEC-009).
 * HTTP stays open; {@code httpBasic} supplies a COORDINATOR principal for scripted CRUD until Phase 6.
 * Not for preprod or production — see {@code TECH_DEBT.md}.
 */
@Configuration
@Profile("docker")
@EnableWebSecurity
@EnableMethodSecurity
public class DockerSecurityConfig {

    @Bean
    SecurityFilterChain dockerSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .httpBasic(httpBasic -> {})
            .build();
    }

    @Bean
    UserDetailsService smokeCoordinatorUserDetailsService(
        @Value("${SMOKE_COORDINATOR_PASSWORD:changeme}") String smokeCoordinatorPassword
    ) {
        UserDetails coordinator = User.builder()
            .username("coordinator")
            .password(passwordEncoder().encode(smokeCoordinatorPassword))
            .roles("COORDINATOR")
            .build();
        return new InMemoryUserDetailsManager(coordinator);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
