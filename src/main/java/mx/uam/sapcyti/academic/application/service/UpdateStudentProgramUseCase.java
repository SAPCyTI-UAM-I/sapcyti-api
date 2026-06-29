package mx.uam.sapcyti.academic.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.application.command.UpdateStudentProgramCommand;
import mx.uam.sapcyti.academic.domain.exception.ProfessorNotFoundException;
import mx.uam.sapcyti.academic.domain.exception.StudentNotFoundException;
import mx.uam.sapcyti.academic.domain.exception.StudentProgramNotFoundException;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.academic.domain.service.ResearchCatalogValidator;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.shared.tenant.TenantAccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateStudentProgramUseCase {

    private final StudentRepositoryPort studentRepository;
    private final StudentProgramRepositoryPort studentProgramRepository;
    private final ProfessorRepositoryPort professorRepository;
    private final GetStudentProgramUseCase getStudentProgramUseCase;
    private final ResearchCatalogValidator researchCatalogValidator;

    @Transactional
    public GetStudentProgramUseCase.StudentProgramDetail execute(UpdateStudentProgramCommand command) {
        Long graduateProgramId = requireTenant();
        assertStudentInTenant(command.studentId(), graduateProgramId);

        StudentProgram program = studentProgramRepository
                .findByIdAndStudentIdAndGraduateProgramId(
                        command.programId(), command.studentId(), graduateProgramId)
                .orElseThrow(StudentProgramNotFoundException::new);

        StudentProgram.validateUniqueAdvisorIds(command.advisorIds());
        assertProfessorExists(command.tutorId(), graduateProgramId);
        for (Long advisorId : command.advisorIds()) {
            assertProfessorExists(advisorId, graduateProgramId);
        }

        researchCatalogValidator.validate(command.lineOfKnowledge(), command.researchArea());

        program.updateMetadata(
                command.admissionDate(),
                command.graduationDate(),
                blankToNull(command.lineOfKnowledge()),
                blankToNull(command.researchArea()),
                command.status(),
                blankToNull(command.withdrawalReason()),
                command.tutorId());
        program.replaceAdvisors(command.advisorIds());

        studentProgramRepository.save(program);

        return getStudentProgramUseCase.execute(command.studentId(), command.programId());
    }

    private void assertStudentInTenant(Long studentId, Long graduateProgramId) {
        studentRepository.findById(studentId)
                .filter(student -> graduateProgramId.equals(student.getGraduateProgramId()))
                .orElseThrow(StudentNotFoundException::new);
    }

    private void assertProfessorExists(Long professorId, Long graduateProgramId) {
        if (professorId == null) {
            return;
        }
        if (!professorRepository.existsByIdAndGraduateProgramId(professorId, graduateProgramId)) {
            throw new ProfessorNotFoundException();
        }
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new TenantAccessDeniedException(TenantAccessDeniedException.MISSING_SCOPE_MESSAGE);
        }
        return graduateProgramId;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
