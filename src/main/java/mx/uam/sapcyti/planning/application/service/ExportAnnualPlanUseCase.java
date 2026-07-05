package mx.uam.sapcyti.planning.application.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.planning.domain.exception.AnnualPlanNotFoundException;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.planning.infrastructure.adapter.out.AnnualPlanExcelExporter;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExportAnnualPlanUseCase {

    private final AnnualPlanRepositoryPort annualPlanRepository;
    private final AnnualPlanExcelExporter exporter;

    @Transactional(readOnly = true)
    public ExportResult execute(int year) {
        Long graduateProgramId = requireTenant();
        AnnualPlan plan = annualPlanRepository
                .findByYearAndGraduateProgramId(year, graduateProgramId)
                .orElseThrow(AnnualPlanNotFoundException::new);

        return new ExportResult("Planeacion PCyTI " + year + ".xlsx", exporter.export(plan));
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
