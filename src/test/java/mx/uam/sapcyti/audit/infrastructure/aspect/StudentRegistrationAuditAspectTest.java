package mx.uam.sapcyti.audit.infrastructure.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import mx.uam.sapcyti.academic.application.service.RegisterStudentUseCase;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.audit.domain.model.AuditEvent;
import mx.uam.sapcyti.audit.domain.port.out.AuditOutputPort;
import java.time.LocalDate;
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
class StudentRegistrationAuditAspectTest {

    @Mock
    private AuditOutputPort auditOutputPort;

    @InjectMocks
    private StudentRegistrationAuditAspect aspect;

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
    @DisplayName("records STUDENT_REGISTERED on successful registration")
    void auditStudentRegistered() {
        RegisterStudentUseCase.RegisterStudentResult result =
                RegisterStudentUseCase.RegisterStudentResult.builder()
                        .id(1L)
                        .userId(20L)
                        .enrollmentId("2123803361")
                        .graduateProgramId(1L)
                        .programType(ProgramType.MAESTRIA)
                        .admissionDate(LocalDate.of(2023, 9, 1))
                        .build();

        aspect.auditStudentRegistered(result);

        verify(auditOutputPort).record(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo("STUDENT_REGISTERED");
        assertThat(event.getActorId()).isEqualTo(1L);
        assertThat(event.getDetails()).contains("studentId=1");
        assertThat(event.getDetails()).contains("enrollmentId=2123803361");
    }
}
