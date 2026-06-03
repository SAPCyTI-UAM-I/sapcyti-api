package mx.uam.sapcyti.identity.infrastructure.adapter.in.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.uam.sapcyti.identity.domain.model.RoleType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private long expiresIn;
    private RoleType role;
}
