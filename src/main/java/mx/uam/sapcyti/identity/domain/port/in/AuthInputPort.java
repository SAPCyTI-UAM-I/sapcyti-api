package mx.uam.sapcyti.identity.domain.port.in;

import lombok.Builder;
import lombok.Getter;
import mx.uam.sapcyti.identity.infrastructure.adapter.in.dto.AuthResponse;

public interface AuthInputPort {
    
    LoginResult login(LoginCommand command);
    
    AuthResponse refresh(String refreshToken);
    
    void logout(String refreshToken);

    @Getter
    @Builder
    class LoginCommand {
        private final String email;
        private final String password;
        private final boolean rememberMe;
        private final String deviceInfo;
    }

    @Getter
    @Builder
    class LoginResult {
        private final AuthResponse authResponse;
        private final String refreshToken;
        private final long refreshExpiresIn;
        private final Long userId;
        private final Long graduateProgramId;
    }
}
