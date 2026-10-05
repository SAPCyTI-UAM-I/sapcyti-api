package mx.uam.sapcyti.academic.domain.service;

import lombok.RequiredArgsConstructor;
import mx.uam.sapcyti.academic.domain.port.out.ResearchCatalogPort;
import org.springframework.stereotype.Service;

/**
 * Validates line/area pairs against the HU-44 catalog (SPEC-019B).
 */
@Service
@RequiredArgsConstructor
public class ResearchCatalogValidator {

    public static final String LINE_REQUIRED_MESSAGE =
            "Line of knowledge is required when research area is selected";
    public static final String AREA_MISMATCH_MESSAGE =
            "Research area does not belong to the selected line of knowledge";

    private final ResearchCatalogPort researchCatalogPort;

    public void validate(String lineOfKnowledge, String researchArea) {
        String line = blankToNull(lineOfKnowledge);
        String area = blankToNull(researchArea);

        if (area != null && line == null) {
            throw new IllegalArgumentException(LINE_REQUIRED_MESSAGE);
        }
        if (line == null) {
            return;
        }
        if (!researchCatalogPort.lineExists(line)) {
            throw new IllegalArgumentException("Unknown line of knowledge: " + line);
        }
        if (area != null && !researchCatalogPort.areaBelongsToLine(line, area)) {
            throw new IllegalArgumentException(AREA_MISMATCH_MESSAGE);
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
