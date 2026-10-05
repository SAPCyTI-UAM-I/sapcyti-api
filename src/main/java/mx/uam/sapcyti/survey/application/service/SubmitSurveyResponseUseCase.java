package mx.uam.sapcyti.survey.application.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.survey.application.command.SubmitResponseCommand;
import mx.uam.sapcyti.survey.domain.exception.BlankWithUeasConflictException;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotActiveException;
import mx.uam.sapcyti.survey.domain.exception.UeaNotAvailableException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.model.StudentSurveyResponse;
import mx.uam.sapcyti.survey.domain.model.SurveyResponseMode;
import mx.uam.sapcyti.survey.domain.model.SurveyStatus;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitSurveyResponseUseCase {

    private final SurveyResponseRepositoryPort responseRepository;
    private final UeaRepositoryPort ueaRepository;
    private final SurveyDetailFactory surveyDetailFactory;
    private final StudentSurveyContextResolver studentContextResolver;

    @Transactional
    public SubmittedResponseView execute(Long surveyId, SubmitResponseCommand command) {
        EnrollmentSurvey survey = surveyDetailFactory.requireSurvey(surveyId);
        if (survey.getStatus(Instant.now()) != SurveyStatus.ACTIVO) {
            throw new SurveyNotActiveException();
        }

        validateCommand(command);

        Long graduateProgramId = survey.getGraduateProgramId();
        Map<Long, UEA> activeById = ueaRepository.findActiveByGraduateProgramId(graduateProgramId).stream()
                .collect(Collectors.toMap(UEA::getId, Function.identity()));

        if (command.mode() == SurveyResponseMode.ENROLL_UEAS) {
            for (Long ueaId : command.ueaIds()) {
                if (!activeById.containsKey(ueaId)) {
                    throw new UeaNotAvailableException();
                }
            }
        }

        Student student = studentContextResolver.requireCurrentStudent();
        List<Long> ueaIds = command.mode() == SurveyResponseMode.BLANK ? List.of() : List.copyOf(command.ueaIds());

        StudentSurveyResponse response = responseRepository
                .findBySurveyIdAndStudentId(surveyId, student.getId())
                .orElseGet(() -> StudentSurveyResponse.create(
                        surveyId, student.getId(), command.academicTerm(), command.mode(), ueaIds));

        response.replace(command.academicTerm(), command.mode(), ueaIds);
        return SubmittedResponseView.from(responseRepository.save(response));
    }

    private static void validateCommand(SubmitResponseCommand command) {
        if (command.mode() == SurveyResponseMode.BLANK) {
            if (command.ueaIds() != null && !command.ueaIds().isEmpty()) {
                throw new BlankWithUeasConflictException();
            }
        } else if (command.ueaIds() == null || command.ueaIds().isEmpty()) {
            throw new IllegalArgumentException("ueaIds must not be empty when mode is ENROLL_UEAS");
        } else if (new HashSet<>(command.ueaIds()).size() != command.ueaIds().size()) {
            throw new IllegalArgumentException("ueaIds must not contain duplicates");
        }
    }
}
