package mx.uam.sapcyti.academic.infrastructure.adapter.out;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.AcademicInformation;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.ProgramStatus;
import mx.uam.sapcyti.academic.domain.model.ProgramType;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.model.StudentExamContext;
import mx.uam.sapcyti.academic.domain.model.StudentExamContextOutcome;
import mx.uam.sapcyti.academic.domain.model.StudentExamContextResult;
import mx.uam.sapcyti.academic.domain.model.StudentProgram;
import mx.uam.sapcyti.academic.domain.port.out.StudentExamContextPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentProgramRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import org.springframework.stereotype.Component;

/**
 * Selects the single {@link StudentProgram} of the authorized graduate program whose {@code programType}
 * equals {@link AcademicInformation#getProgramType()}. Zero or several matches are ambiguous; admission date
 * is not a tie-break. A stored name outside {@link ProgramType} or {@link ProgramStatus} cannot be loaded as
 * a domain enum, so that persistence failure is {@code DEGREE_UNSUPPORTED} or {@code STATUS_UNSUPPORTED}
 * instead of a partial row.
 */
@Component
@RequiredArgsConstructor
public class StudentExamContextAdapter implements StudentExamContextPort {

    private static final String UNKNOWN_ENUM = "No enum constant ";

    private final StudentRepositoryPort studentRepository;
    private final StudentProgramRepositoryPort studentProgramRepository;

    @Override
    public StudentExamContextResult resolve(Long userId, Long graduateProgramId) {
        Optional<Student> student;
        try {
            student = studentRepository.findByUserIdAndGraduateProgramId(userId, graduateProgramId);
        } catch (RuntimeException ex) {
            return unrecognizedOrRethrow(ex);
        }
        if (student.isEmpty()) {
            return StudentExamContextResult.of(StudentExamContextOutcome.NOT_FOUND);
        }

        ProgramType profileType = profileType(student.get());
        List<StudentProgram> programs;
        try {
            programs = studentProgramRepository.findByStudentIdAndGraduateProgramId(
                    student.get().getId(), graduateProgramId);
        } catch (RuntimeException ex) {
            return unrecognizedOrRethrow(ex);
        }

        List<StudentProgram> matches = programs.stream()
                .filter(program -> profileType != null && program.getProgramType() == profileType)
                .toList();
        if (matches.size() != 1) {
            return StudentExamContextResult.of(StudentExamContextOutcome.AMBIGUOUS);
        }

        StudentProgram match = matches.getFirst();
        if (match.getStatus() == null) {
            return StudentExamContextResult.of(StudentExamContextOutcome.STATUS_UNSUPPORTED);
        }

        PersonalData personalData = student.get().getPersonalData();
        if (missingOfficialName(personalData)) {
            return StudentExamContextResult.of(StudentExamContextOutcome.INCOMPLETE);
        }

        return StudentExamContextResult.resolved(new StudentExamContext(
                student.get().getId(),
                match.getGraduateProgramId(),
                match.getId(),
                match.getProgramType(),
                match.getStatus(),
                personalData.getFirstName(),
                personalData.getFirstLastName(),
                maternalSurname(personalData.getSecondLastName())));
    }

    private static StudentExamContextResult unrecognizedOrRethrow(RuntimeException error) {
        return unrecognizedOutcome(error)
                .map(StudentExamContextResult::of)
                .orElseThrow(() -> error);
    }

    private static Optional<StudentExamContextOutcome> unrecognizedOutcome(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(UNKNOWN_ENUM + ProgramType.class.getName() + ".")) {
                return Optional.of(StudentExamContextOutcome.DEGREE_UNSUPPORTED);
            }
            if (message != null && message.contains(UNKNOWN_ENUM + ProgramStatus.class.getName() + ".")) {
                return Optional.of(StudentExamContextOutcome.STATUS_UNSUPPORTED);
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private static ProgramType profileType(Student student) {
        AcademicInformation academicInformation = student.getAcademicInformation();
        if (academicInformation == null) {
            return null;
        }
        return academicInformation.getProgramType();
    }

    private static boolean missingOfficialName(PersonalData personalData) {
        return personalData == null
                || isBlank(personalData.getFirstName())
                || isBlank(personalData.getFirstLastName());
    }

    private static String maternalSurname(String secondLastName) {
        if (isBlank(secondLastName)) {
            return null;
        }
        return secondLastName;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
