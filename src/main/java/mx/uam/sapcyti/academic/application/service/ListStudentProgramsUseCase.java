package mx.uam.sapcyti.academic.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListStudentProgramsUseCase {

    private final StudentRepositoryPort studentRepository;
    private final StudentProgramRepositoryPort studentProgramRepository;

    @Transactional(readOnly = true)
    public List<StudentProgramSummaryItem> execute(Long studentId) {
        Long graduateProgramId = requireTenant();

        assertStudentInTenant(studentId, graduateProgramId);

        return studentProgramRepository.findByStudentIdAndGraduateProgramId(studentId, graduateProgramId)
                .stream()
                .map(StudentProgramSummaryItem::from)
                .toList();
    }

    private void assertStudentInTenant(Long studentId, Long graduateProgramId) {
        studentRepository.findById(studentId)
                .filter(student -> graduateProgramId.equals(student.getGraduateProgramId()))
                .orElseThrow(StudentNotFoundException::new);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }

    public record StudentProgramSummaryItem(
            Long id,
            ProgramType programType,
            String enrollmentId,
            ProgramStatus status,
            Long tutorId,
            boolean hasTutor) {

        static StudentProgramSummaryItem from(StudentProgram program) {
            return new StudentProgramSummaryItem(
                    program.getId(),
                    program.getProgramType(),
                    program.getEnrollmentId(),
                    program.getStatus(),
                    program.getTutorId(),
                    program.getTutorId() != null);
        }
    }
}
