package mx.uam.sapcyti.trimestral.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.out.excel.TrimestralPlanExcelExporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportTrimestralPlanUseCaseTest {

    @Mock
    private TrimestralPlanRepositoryPort repository;

    @Mock
    private TrimestralPlanExcelExporter exporter;

    @Mock
    private TrimestralPrerequisiteGuard prerequisiteGuard;

    @BeforeEach
    void setTenant() {
        TenantContext.set(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void exporterFailureDoesNotAlterExportedAt() {
        TrimestralPlan plan = TrimestralPlan.create(1L, 2L, "26O", 5L);
        when(repository.findByIdAndGraduateProgramId(8L, 1L))
                .thenReturn(Optional.of(plan));
        when(exporter.export(plan)).thenThrow(new IllegalStateException("workbook failed"));
        ExportTrimestralPlanUseCase useCase =
                new ExportTrimestralPlanUseCase(repository, exporter, prerequisiteGuard);

        assertThatThrownBy(() -> useCase.execute(8L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("workbook failed");

        assertThat(plan.getExportedAt()).isNull();
        verify(repository, never()).save(plan);
    }

    @Test
    void successfulDraftExportUpdatesExportedAtAfterWorkbookExists() {
        TrimestralPlan plan = TrimestralPlan.create(1L, 2L, "26O", 5L);
        when(repository.findByIdAndGraduateProgramId(8L, 1L))
                .thenReturn(Optional.of(plan));
        when(exporter.export(plan)).thenReturn(new byte[] {1, 2, 3});
        ExportTrimestralPlanUseCase useCase =
                new ExportTrimestralPlanUseCase(repository, exporter, prerequisiteGuard);

        var result = useCase.execute(8L);

        assertThat(result.content()).containsExactly(1, 2, 3);
        assertThat(result.filename()).isEqualTo("PCYTI 26O.xlsx");
        assertThat(plan.getExportedAt()).isNotNull();
        verify(repository).save(plan);
    }
}
