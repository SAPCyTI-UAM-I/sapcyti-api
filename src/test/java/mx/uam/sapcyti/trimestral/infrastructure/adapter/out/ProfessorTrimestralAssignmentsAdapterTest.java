package mx.uam.sapcyti.trimestral.infrastructure.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import mx.uam.sapcyti.trimestral.domain.model.GroupProfessor;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanGroup;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlanStatus;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProfessorTrimestralAssignmentsAdapterTest {

    @Mock
    private TrimestralPlanRepositoryPort repository;

    @Test
    void listsAndRefreshesOnlyOpenPlanSnapshots() {
        TrimestralPlan open = planWithProfessor(10L, TrimestralPlanStatus.BORRADOR, "Original open");
        TrimestralPlan terminated =
                planWithProfessor(20L, TrimestralPlanStatus.TERMINADA, "Original frozen");
        when(repository.findAllByGraduateProgramId(1L)).thenReturn(List.of(open, terminated));
        ProfessorTrimestralAssignmentsAdapter adapter =
                new ProfessorTrimestralAssignmentsAdapter(repository);

        assertThat(adapter.findOpenGroupAssignments(7L, 1L))
                .singleElement()
                .satisfies(assignment -> {
                    assertThat(assignment.planId()).isEqualTo(10L);
                    assertThat(assignment.term()).isEqualTo("26O");
                    assertThat(assignment.clave()).isEqualTo("2156041");
                    assertThat(assignment.grupo()).isEqualTo("CO43");
                });

        adapter.refreshOpenPlanSnapshots(7L, 1L, "NEW-NEMP", "Updated Name");

        assertThat(open.getGroups().getFirst().getProfessors().getFirst().getEmployeeNumber())
                .isEqualTo("NEW-NEMP");
        assertThat(open.getGroups().getFirst().getProfessors().getFirst().getProfessorName())
                .isEqualTo("Updated Name");
        assertThat(terminated.getGroups().getFirst().getProfessors().getFirst().getProfessorName())
                .isEqualTo("Original frozen");
        verify(repository).save(open);
        verify(repository, never()).save(terminated);
    }

    private static TrimestralPlan planWithProfessor(
            Long id, TrimestralPlanStatus status, String professorName) {
        TrimestralPlan plan = TrimestralPlan.create(1L, 3L, "26O", 5L);
        ReflectionTestUtils.setField(plan, "id", id);
        if (status == TrimestralPlanStatus.TERMINADA) {
            plan.transitionTo(status);
        }
        TrimestralPlanGroup group = TrimestralPlanGroup.createProposed(
                plan,
                4L,
                (short) 1,
                "2156041",
                "MÉTODOS",
                "OBLIGATORIA",
                "CO43",
                "25",
                "*");
        group.replaceProfessors(List.of(
                GroupProfessor.create(group, 7L, "OLD-NEMP", professorName, (short) 1)));
        plan.replaceGroups(List.of(group));
        return plan;
    }
}
