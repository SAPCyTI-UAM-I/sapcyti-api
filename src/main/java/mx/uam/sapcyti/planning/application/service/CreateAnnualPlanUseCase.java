package mx.uam.sapcyti.planning.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.identity.infrastructure.security.AuthenticatedUserResolver;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.planning.domain.exception.AnnualPlanAlreadyExistsException;
import mx.uam.sapcyti.planning.domain.model.AnnualPlan;
import mx.uam.sapcyti.planning.domain.model.AnnualPlanEntry;
import mx.uam.sapcyti.planning.domain.port.out.AnnualPlanRepositoryPort;
import mx.uam.sapcyti.shared.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAnnualPlanUseCase {

    private final AnnualPlanRepositoryPort annualPlanRepository;
    private final UeaRepositoryPort ueaRepository;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Transactional
    public AnnualPlan execute(int year) {
        Long graduateProgramId = requireTenant();
        if (annualPlanRepository.existsByYearAndGraduateProgramId(year, graduateProgramId)) {
            throw new AnnualPlanAlreadyExistsException();
        }

        List<UEA> activeUeas = ueaRepository.findActiveByGraduateProgramId(graduateProgramId);
        Map<Long, AnnualPlanEntry> previousEntries = annualPlanRepository
                .findByYearAndGraduateProgramId(year - 1, graduateProgramId)
                .map(plan -> plan.getEntries().stream()
                        .collect(Collectors.toMap(AnnualPlanEntry::getUeaId, Function.identity())))
                .orElse(Map.of());

        Long createdBy = authenticatedUserResolver.resolve().getUserId();
        AnnualPlan plan = AnnualPlan.create(graduateProgramId, year, createdBy, activeUeas, previousEntries);
        return annualPlanRepository.save(plan);
    }

    private static Long requireTenant() {
        Long graduateProgramId = TenantContext.get();
        if (graduateProgramId == null) {
            throw new IllegalStateException("Graduate program context is required");
        }
        return graduateProgramId;
    }
}
