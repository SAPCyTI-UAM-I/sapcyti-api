package mx.uam.sapcyti.audit.infrastructure.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import mx.uam.sapcyti.academic.application.service.RegisterProfessorUseCase;
import mx.uam.sapcyti.academic.application.service.ListProfessorsUseCase;
import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class ProfessorRegistrationAuditAspectTest {

    @Mock
    private AuditOutputPort auditOutputPort;

    @InjectMocks
    private ProfessorRegistrationAuditAspect aspect;

    @Captor
    private ArgumentCaptor<AuditEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "1",
                        null,
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_COORDINATOR"))));
    }

    @Test
    @DisplayName("records PROFESSOR_REGISTERED on successful registration")
    void auditProfessorRegistered() {
        RegisterProfessorUseCase.RegisterProfessorResult result =
                RegisterProfessorUseCase.RegisterProfessorResult.builder()
                        .id(10L)
                        .userId(20L)
                        .employeeNumber("30568")
                        .graduateProgramId(1L)
                        .build();

        aspect.auditProfessorRegistered(result);

        verify(auditOutputPort).record(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo("PROFESSOR_REGISTERED");
        assertThat(event.getActorId()).isEqualTo(1L);
        assertThat(event.getDetails()).contains("professorId=10");
        assertThat(event.getDetails()).contains("employeeNumber=30568");
    }

    @Test
    @DisplayName("records PROFESSOR_RESTORED on successful restoration")
    void auditProfessorRestored() {
        ListProfessorsUseCase.ProfessorListItem result =
                ListProfessorsUseCase.ProfessorListItem.builder()
                        .id(10L)
                        .userId(20L)
                        .employeeNumber("12345")
                        .graduateProgramId(1L)
                        .build();

        aspect.auditProfessorRestored(result);

        verify(auditOutputPort).record(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo("PROFESSOR_RESTORED");
        assertThat(event.getActorId()).isEqualTo(1L);
        assertThat(event.getDetails()).contains("professorId=10");
        assertThat(event.getDetails()).contains("employeeNumber=12345");
    }
}
