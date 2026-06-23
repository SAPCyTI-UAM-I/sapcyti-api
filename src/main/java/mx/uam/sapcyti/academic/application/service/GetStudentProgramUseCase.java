package mx.uam.sapcyti.academic.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.exception.StudentProgramNotFoundException;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Professor;
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
public class GetStudentProgramUseCase {

    private final StudentRepositoryPort studentRepository;
    private final StudentProgramRepositoryPort studentProgramRepository;
    private final ProfessorRepositoryPort professorRepository;

    @Transactional(readOnly = true)
    public StudentProgramDetail execute(Long studentId, Long programId) {
        Long graduateProgramId = requireTenant();
        assertStudentInTenant(studentId, graduateProgramId);

        StudentProgram program = studentProgramRepository
                .findByIdAndStudentIdAndGraduateProgramId(programId, studentId, graduateProgramId)
                .orElseThrow(StudentProgramNotFoundException::new);

        return StudentProgramDetail.from(program, graduateProgramId, professorRepository);
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

    public record ProfessorReference(
            Long id,
            String firstName,
            String firstLastName,
            String secondLastName) {

        static ProfessorReference from(Professor professor) {
            PersonalData personalData = professor.getPersonalData();
            return new ProfessorReference(
                    professor.getId(),
                    personalData.getFirstName(),
                    personalData.getFirstLastName(),
                    personalData.getSecondLastName());
        }
    }

    public record StudentProgramDetail(
            Long id,
            Long studentId,
            Long graduateProgramId,
            String enrollmentId,
            ProgramType programType,
            LocalDate admissionDate,
            LocalDate graduationDate,
            String researchArea,
            ProgramStatus status,
            String withdrawalReason,
            Long tutorId,
            ProfessorReference tutor,
            List<Long> advisorIds,
            List<ProfessorReference> advisors) {

        static StudentProgramDetail from(
                StudentProgram program,
                Long graduateProgramId,
                ProfessorRepositoryPort professorRepository) {
            ProfessorReference tutor = resolveProfessor(program.getTutorId(), graduateProgramId, professorRepository);
            List<Long> advisorIds = program.getAdvisorIds();
            List<ProfessorReference> advisors = resolveProfessors(advisorIds, graduateProgramId, professorRepository);

            return new StudentProgramDetail(
                    program.getId(),
                    program.getStudentId(),
                    program.getGraduateProgramId(),
                    program.getEnrollmentId(),
                    program.getProgramType(),
                    program.getAdmissionDate(),
                    program.getGraduationDate(),
                    program.getResearchArea(),
                    program.getStatus(),
                    program.getWithdrawalReason(),
                    program.getTutorId(),
                    tutor,
                    advisorIds,
                    advisors);
        }

        private static ProfessorReference resolveProfessor(
                Long professorId,
                Long graduateProgramId,
                ProfessorRepositoryPort professorRepository) {
            if (professorId == null) {
                return null;
            }
            return professorRepository.findById(professorId)
                    .filter(professor -> graduateProgramId.equals(professor.getGraduateProgramId()))
                    .map(ProfessorReference::from)
                    .orElse(null);
        }

        private static List<ProfessorReference> resolveProfessors(
                List<Long> professorIds,
                Long graduateProgramId,
                ProfessorRepositoryPort professorRepository) {
            List<ProfessorReference> references = new ArrayList<>();
            for (Long professorId : professorIds) {
                professorRepository.findById(professorId)
                        .filter(professor -> graduateProgramId.equals(professor.getGraduateProgramId()))
                        .map(ProfessorReference::from)
                        .ifPresent(references::add);
            }
            return references;
        }
    }
}
