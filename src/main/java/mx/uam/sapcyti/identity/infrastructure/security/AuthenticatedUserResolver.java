package mx.uam.sapcyti.identity.infrastructure.security;

import mx.uam.sapcyti.identity.application.model.AuthenticatedUser;
import mx.uam.sapcyti.identity.domain.model.RoleType;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserResolver {

    public AuthenticatedUser resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in security context");
        }

        Long userId = Long.parseLong(authentication.getName());
        RoleType role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replace("ROLE_", ""))
                .map(RoleType::valueOf)
                .orElseThrow(() -> new IllegalStateException("Authenticated user has no role"));

        return AuthenticatedUser.builder()
                .userId(userId)
                .role(role)
                .graduateProgramId(TenantContext.get())
                .build();
    }
}
