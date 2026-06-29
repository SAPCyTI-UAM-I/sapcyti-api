package mx.uam.sapcyti.audit.infrastructure.aspect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import mx.uam.sapcyti.offering.application.service.RegisterUeaUseCase;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class UeaAuditAspectTest {

    @Mock
    private AuditOutputPort auditOutputPort;

    @InjectMocks
    private UeaAuditAspect aspect;

    @Test
    @DisplayName("records UEA_REGISTERED after successful registration")
    void auditRegistered() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "1", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_COORDINATOR"))));

        RegisterUeaUseCase.RegisterUeaResult result = RegisterUeaUseCase.RegisterUeaResult.builder()
                .id(10L)
                .clave("2156041")
                .nombre("Nombre")
                .tipo(UeaType.OPTATIVA)
                .modalidad(UeaModality.MIXTA)
                .horasTeoria(new BigDecimal("3"))
                .horasPractica(new BigDecimal("3"))
                .tipoFormacion(FormationType.COMPLEMENTARIA)
                .creditos(9)
                .active(true)
                .graduateProgramId(1L)
                .build();

        aspect.auditUeaRegistered(result);

        verify(auditOutputPort).record(any());
        SecurityContextHolder.clearContext();
    }
}
