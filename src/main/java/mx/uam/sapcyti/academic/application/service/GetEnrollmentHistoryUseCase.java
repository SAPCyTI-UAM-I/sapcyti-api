package mx.uam.sapcyti.academic.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort;
import mx.uam.sapcyti.academic.domain.port.out.EnrollmentHistoryPort.EnrollmentHistoryEntry;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetEnrollmentHistoryUseCase {

    private final StudentRepositoryPort studentRepository;
    private final EnrollmentHistoryPort enrollmentHistoryPort;

    @Transactional(readOnly = true)
    public List<EnrollmentHistoryEntry> execute(Long studentId) {
        Long graduateProgramId = requireTenant();

        Student student = studentRepository.findById(studentId).orElseThrow(StudentNotFoundException::new);
        if (!graduateProgramId.equals(student.getGraduateProgramId())) {
            throw new StudentNotFoundException();
        }

        return enrollmentHistoryPort.findByStudent(studentId, graduateProgramId);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }
}
