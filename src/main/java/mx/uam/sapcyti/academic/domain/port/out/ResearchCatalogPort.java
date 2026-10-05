package mx.uam.sapcyti.academic.domain.port.out;

import java.util.List;

/**
 * Read-only access to the HU-44 research catalog.
 */
public interface ResearchCatalogPort {

    List<ResearchCatalogEntry> findAll();

    boolean lineExists(String lineName);

    boolean areaBelongsToLine(String lineName, String areaName);

    record ResearchCatalogEntry(String line, List<String> areas) {
    }
}
