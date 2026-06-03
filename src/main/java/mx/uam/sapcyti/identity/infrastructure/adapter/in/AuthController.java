package mx.uam.sapcyti.identity.infrastructure.adapter.in;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.identity.domain.exception.InvalidRefreshTokenException;
import mx.uam.sapcyti.identity.domain.port.in.AuthInputPort;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.AuthResponse;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthInputPort authInputPort;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthInputPort.LoginCommand command = AuthInputPort.LoginCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .rememberMe(request.isRememberMe())
                .deviceInfo(request.getDeviceInfo())
                .build();

        AuthInputPort.LoginResult result = authInputPort.login(command);
        
        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(isSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(result.getRefreshExpiresIn())
                .build();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.getAuthResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        AuthResponse authResponse = authInputPort.refresh(refreshToken);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authInputPort.logout(refreshToken);
        }
        
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .maxAge(0)
                .path("/api/auth")
                .build();
                
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

    private boolean isSecure() {
        return !activeProfiles.contains("dev") && !activeProfiles.contains("local");
    }
}
