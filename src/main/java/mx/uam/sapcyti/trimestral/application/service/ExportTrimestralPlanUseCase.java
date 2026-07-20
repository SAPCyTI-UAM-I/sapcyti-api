package mx.uam.sapcyti.trimestral.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import mx.uam.sapcyti.trimestral.domain.exception.TrimestralPlanNotFoundException;
import mx.uam.sapcyti.trimestral.domain.model.TrimestralPlan;
import mx.uam.sapcyti.trimestral.domain.port.out.TrimestralPlanRepositoryPort;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.out.excel.TrimestralExcelLayout;
import mx.uam.sapcyti.trimestral.infrastructure.adapter.out.excel.TrimestralPlanExcelExporter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportTrimestralPlanUseCase {

    private final TrimestralPlanRepositoryPort planRepository;
    private final TrimestralPlanExcelExporter exporter;

    // Not readOnly: the @AfterReturning audit aspect may insert within this transaction.
    @Transactional
    public ExportResult execute(Long id) {
        Long graduateProgramId = requireTenant();
        TrimestralPlan plan = planRepository
                .findByIdAndGraduateProgramId(id, graduateProgramId)
                .orElseThrow(TrimestralPlanNotFoundException::new);

        return new ExportResult(TrimestralExcelLayout.filename(plan.getTerm()), exporter.export(plan));
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }

    public record ExportResult(String filename, byte[] content) {}
}
