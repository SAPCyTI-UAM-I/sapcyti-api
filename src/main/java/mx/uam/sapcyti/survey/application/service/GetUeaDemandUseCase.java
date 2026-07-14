package mx.uam.sapcyti.survey.application.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.offering.domain.model.UEA;
import mx.uam.sapcyti.offering.domain.port.out.UeaRepositoryPort;
import mx.uam.sapcyti.survey.domain.port.out.SurveyResponseRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetUeaDemandUseCase {

    private static final String SORT_FIELD = "totalResponses";

    private final UeaRepositoryPort ueaRepository;
    private final SurveyResponseRepositoryPort responseRepository;
    private final SurveyDetailFactory surveyDetailFactory;

    @Transactional(readOnly = true)
    public List<UeaDemandRow> execute(Long surveyId, String sort) {
        var survey = surveyDetailFactory.requireSurvey(surveyId);
        Long graduateProgramId = survey.getGraduateProgramId();
        SortDirection direction = parseSort(sort);

        Map<Long, Long> counts = responseRepository.countResponsesByUeaId(surveyId).stream()
                .collect(Collectors.toMap(
                        SurveyResponseRepositoryPort.UeaDemandAggregate::ueaId,
                        SurveyResponseRepositoryPort.UeaDemandAggregate::totalResponses));

        if (counts.isEmpty()) {
            return List.of();
        }

        List<UeaDemandRow> rows = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : counts.entrySet()) {
            UEA uea = ueaRepository.findByIdAndGraduateProgramId(entry.getKey(), graduateProgramId)
                    .orElse(null);
            rows.add(UeaDemandRow.builder()
                    .ueaId(entry.getKey())
                    .clave(uea != null ? uea.getClave() : String.valueOf(entry.getKey()))
                    .nombre(uea != null ? uea.getNombre() : "")
                    .tipoFormacion(uea != null ? uea.getTipoFormacion().name() : "")
                    .creditos(uea != null ? uea.getCreditos() : 0)
                    .totalResponses(entry.getValue())
                    .build());
        }

        Comparator<UeaDemandRow> comparator = Comparator.comparingLong(UeaDemandRow::getTotalResponses);
        if (direction == SortDirection.DESC) {
            comparator = comparator.reversed();
        }
        return rows.stream().sorted(comparator).toList();
    }

    private static SortDirection parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return SortDirection.DESC;
        }
        String[] parts = sort.split(",");
        if (parts.length != 2 || !SORT_FIELD.equals(parts[0].trim())) {
            throw new IllegalArgumentException("sort must be totalResponses,asc or totalResponses,desc");
        }
        String dir = parts[1].trim().toLowerCase();
        if ("asc".equals(dir)) {
            return SortDirection.ASC;
        }
        if ("desc".equals(dir)) {
            return SortDirection.DESC;
        }
        throw new IllegalArgumentException("sort must be totalResponses,asc or totalResponses,desc");
    }

    private enum SortDirection {
        ASC,
        DESC
    }
}
