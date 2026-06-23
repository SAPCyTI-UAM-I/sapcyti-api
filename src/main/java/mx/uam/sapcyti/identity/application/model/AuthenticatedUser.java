package mx.uam.sapcyti.identity.application.model;

import lombok.Builder;
import lombok.Value;
import mx.uam.sapcyti.identity.domain.model.RoleType;

@Value
@Builder
public class AuthenticatedUser {

    Long userId;
    RoleType role;
    Long graduateProgramId;
}
