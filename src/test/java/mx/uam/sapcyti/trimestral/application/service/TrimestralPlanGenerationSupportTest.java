package mx.uam.sapcyti.trimestral.application.service;

import static mx.uam.sapcyti.academic.AcademicTestFixtures.sampleAcademicInformation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mx.uam.sapcyti.academic.domain.model.PersonalData;
import mx.uam.sapcyti.academic.domain.model.Student;
import mx.uam.sapcyti.academic.domain.port.out.ProfessorRepositoryPort;
import mx.uam.sapcyti.academic.domain.port.out.StudentRepositoryPort;
import mx.uam.sapcyti.identity.domain.port.out.UserRepositoryPort;
import mx.uam.sapcyti.offering.domain.model.FormationType;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.model.UeaModality;
import mx.uam.sapcyti.offering.domain.model.UeaType;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanQuota;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.survey.domain.model.AcademicTerm;
import mx.uam.sapcyti.survey.domain.model.StudentSurveyResponse;
import mx.uam.sapcyti.survey.domain.model.SurveyResponseMode;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.UnassignedDemandReason;
import mx.uam.sapcyti.trimestral.domain.service.GroupLetterService;
import mx.uam.sapcyti.trimestral.domain.service.WarningEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TrimestralPlanGenerationSupportTest {

    @Mock
    private SurveyResponseRepositoryPort surveyResponses;

    @Mock
    private AnnualPlanRepositoryPort annualPlans;

    @Mock
    private UeaRepositoryPort ueas;

    @Mock
    private StudentRepositoryPort students;

    @Mock
    private ProfessorRepositoryPort professors;

    @Mock
    private UserRepositoryPort users;

    @Test
    void suffixStopsAtZAndPersistsRemainingDemand() {
        TrimestralPlan plan = populate(28, "*", "1");

        assertThat(plan.getGroups()).hasSize(27);
        assertThat(plan.getGroups().getFirst().getGrupo()).isEqualTo("CO43");
        assertThat(plan.getGroups().getLast().getGrupo()).isEqualTo("CO43Z");
        assertThat(plan.getGroups())
                .extracting(group -> group.getGrupo())
                .noneMatch(code -> code.contains("["));
        assertThat(plan.getUnassignedDemand())
                .singleElement()
                .satisfies(demand -> assertThat(demand.getReason())
                        .isEqualTo(UnassignedDemandReason.GROUP_SUFFIX_LIMIT));
    }

    @Test
    void numericMaximumLeavesCapacityOverflowUnassigned() {
        TrimestralPlan plan = populate(16, "1", "15");

        assertThat(plan.getGroups()).singleElement()
                .satisfies(group -> assertThat(group.getStudents()).hasSize(15));
        assertThat(plan.getUnassignedDemand()).singleElement()
                .satisfies(demand -> assertThat(demand.getReason())
                        .isEqualTo(UnassignedDemandReason.GROUP_LIMIT_REACHED));
    }

    @Test
    void wildcardGroupsWithCupoOneCreatesOneGroupPerStudent() {
        TrimestralPlan plan = populate(16, "*", "1");

        assertThat(plan.getGroups()).hasSize(16);
        assertThat(plan.getGroups()).allSatisfy(
                group -> assertThat(group.getStudents()).hasSize(1));
        assertThat(plan.getUnassignedDemand()).isEmpty();
    }

    @Test
    void numericFourteenGroupsLeavesTwoStudentsUnassigned() {
        TrimestralPlan plan = populate(16, "14", "1");

        assertThat(plan.getGroups()).hasSize(14);
        assertThat(plan.getUnassignedDemand()).hasSize(2).allSatisfy(
                demand -> assertThat(demand.getReason())
                        .isEqualTo(UnassignedDemandReason.GROUP_LIMIT_REACHED));
    }

    @Test
    void doubleWildcardUsesOneGroupForAllDemand() {
        TrimestralPlan plan = populate(16, "*", "*");

        assertThat(plan.getGroups()).singleElement()
                .satisfies(group -> assertThat(group.getStudents()).hasSize(16));
        assertThat(plan.getUnassignedDemand()).isEmpty();
    }

    private TrimestralPlan populate(int studentCount, String maxGroups, String capacity) {
        long ueaId = 90L;
        UEA uea = UEA.create(
                1L,
                "2156090",
                "PROYECTO",
                UeaType.OBLIGATORIA,
                UeaModality.MIXTA,
                new BigDecimal("3"),
                new BigDecimal("3"),
                FormationType.INVESTIGACION,
                9);
        ReflectionTestUtils.setField(uea, "id", ueaId);

        List<Student> catalog = new ArrayList<>();
        List<StudentSurveyResponse> responses = new ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            long studentId = i + 1L;
            Student student = new Student(
                    String.format("212300%04d", i),
                    100L + i,
                    1L,
                    null,
                    new PersonalData(
                            "Name",
                            String.format("Last%02d", i),
                            "Second",
                            "Mexicana",
                            null,
                            "5550000000",
                            null),
                    sampleAcademicInformation());
            ReflectionTestUtils.setField(student, "id", studentId);
            catalog.add(student);
            responses.add(StudentSurveyResponse.create(
                    7L,
                    studentId,
                    AcademicTerm.I,
                    SurveyResponseMode.ENROLL_UEAS,
                    List.of(ueaId)));
        }
        when(surveyResponses.findAllBySurveyId(7L)).thenReturn(responses);
        when(students.findByGraduateProgramId(1L)).thenReturn(catalog);
        when(annualPlans.findQuotas(2026, 1L))
                .thenReturn(List.of(new AnnualPlanQuota(
                        ueaId, null, null, null, null, maxGroups, capacity)));
        when(ueas.findByIdAndGraduateProgramId(ueaId, 1L)).thenReturn(Optional.of(uea));

        TrimestralPlanGenerationSupport support = new TrimestralPlanGenerationSupport(
                surveyResponses,
                annualPlans,
                ueas,
                students,
                professors,
                users,
                new TrimestralDemandAllocator(new GroupLetterService()),
                new WarningEngine());
        TrimestralPlan plan = TrimestralPlan.create(1L, 7L, "26O", 5L);

        support.populateFromSurvey(plan);
        return plan;
    }
}
