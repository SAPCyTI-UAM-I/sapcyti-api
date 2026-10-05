package mx.uam.sapcyti.survey.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.survey.domain.exception.SurveyNotFoundException;
import mx.uam.sapcyti.survey.domain.model.EnrollmentSurvey;
import mx.uam.sapcyti.survey.domain.port.out.EnrollmentSurveyRepositoryPort;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetActiveSurveyForStudentUseCase {

    private final EnrollmentSurveyRepositoryPort surveyRepository;
    private final UeaRepositoryPort ueaRepository;
    private final SurveyResponseRepositoryPort responseRepository;
    private final SurveyDetailFactory surveyDetailFactory;
    private final StudentSurveyContextResolver studentContextResolver;

    @Transactional(readOnly = true)
    public StudentSurveyForm execute() {
        Long graduateProgramId = SurveyDetailFactory.requireTenant();
        EnrollmentSurvey survey = surveyRepository
                .findActiveByGraduateProgramId(graduateProgramId, Instant.now())
                .orElseThrow(SurveyNotFoundException::new);

        Student student = studentContextResolver.requireCurrentStudent();
        List<UEA> activeUeas = ueaRepository.findActiveByGraduateProgramId(graduateProgramId);
        Map<Long, UEA> activeById = activeUeas.stream()
                .collect(Collectors.toMap(UEA::getId, Function.identity()));

        Set<Long> snapshotIds = survey.getSnapshotUeaIds();
        List<String> removedClaves = new ArrayList<>();
        for (Long snapshotUeaId : snapshotIds) {
            if (!activeById.containsKey(snapshotUeaId)) {
                ueaRepository.findByIdAndGraduateProgramId(snapshotUeaId, graduateProgramId)
                        .ifPresent(uea -> removedClaves.add(uea.getClave()));
            }
        }

        SubmittedResponseView myResponse = responseRepository
                .findBySurveyIdAndStudentId(survey.getId(), student.getId())
                .map(SubmittedResponseView::from)
                .orElse(null);

        String fullName = buildFullName(student);

        return StudentSurveyForm.builder()
                .survey(surveyDetailFactory.toDetail(survey))
                .student(StudentSurveyForm.StudentInfo.builder()
                        .fullName(fullName)
                        .enrollmentId(student.getEnrollmentId())
                        .programType(student.getAcademicInformation().getProgramType())
                        .build())
                .availableUeas(activeUeas.stream().map(StudentSurveyForm.UeaSummary::from).toList())
                .removedUeaClaves(removedClaves)
                .myResponse(myResponse)
                .build();
    }

    private static String buildFullName(Student student) {
        String second = student.getPersonalData().getSecondLastName();
        String base = student.getPersonalData().getFirstName() + " "
                + student.getPersonalData().getFirstLastName();
        if (second != null && !second.isBlank()) {
            return base + " " + second;
        }
        return base;
    }
}
