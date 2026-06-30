package mx.uam.sapcyti.audit.infrastructure.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import mx.uam.sapcyti.academic.application.service.GetStudentProgramUseCase;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
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
class StudentProgramAuditAspectTest {

    @Mock
    private AuditOutputPort auditOutputPort;

    @InjectMocks
    private StudentProgramAuditAspect aspect;

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
                        List.of(new SimpleGrantedAuthority("ROLE_COORDINATOR"))));
    }

    @Test
    @DisplayName("HU-20: records STUDENT_PROGRAM_UPDATED on successful update")
    void auditStudentProgramUpdated() {
        GetStudentProgramUseCase.StudentProgramDetail detail =
                new GetStudentProgramUseCase.StudentProgramDetail(
                        100L, 50L, 1L, "2123803361", ProgramType.MAESTRIA,
                        LocalDate.of(2023, 9, 1), null, null, null, ProgramStatus.ACTIVO, null,
                        10L, null, List.of(11L), List.of());

        aspect.auditStudentProgramUpdated(detail);

        verify(auditOutputPort).record(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();
        assertThat(event.getAction()).isEqualTo("STUDENT_PROGRAM_UPDATED");
        assertThat(event.getDetails()).contains("programId=100");
        assertThat(event.getDetails()).contains("studentId=50");
    }
}
