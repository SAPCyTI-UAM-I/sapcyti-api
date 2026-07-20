package mx.uam.sapcyti.trimestral.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Pure domain service that proposes official group codes from the survey-declared
 * academic term (HU-57). Proposal only — never re-imposed on manual save (HU-59).
 */
@Service
public class GroupLetterService {

    /**
     * Maps academic term number 1..9 → letters O..W; empty for n &lt; 1 or n ≥ 10.
     */
    public Optional<Character> letterForTerm(int academicTermNumber) {
        if (academicTermNumber < 1 || academicTermNumber > 9) {
            return Optional.empty();
        }
        return Optional.of((char) ('A' + 13 + academicTermNumber));
    }

    /**
     * Official base group: fixed {@code C} + letter + graduate-program clave {@code 43}.
     */
    public String baseGroup(char letter) {
        return "C" + letter + "43";
    }

    /**
     * When several groups share the same base (UEA with cupo 1 requested by multiple
     * students), sort by surnames and apply suffixes {@code ""}, {@code A}, {@code B}, …
     *
     * @param sameBase groups that already share one base group code; order is ignored
     * @return proposed {@code grupo} values in surname sort order (same length as input)
     */
    public List<String> assignSuffixes(List<Group> sameBase) {
        if (sameBase == null || sameBase.isEmpty()) {
            return List.of();
        }

        String base = sameBase.getFirst().baseGroup();
        List<Group> sorted = sameBase.stream()
                .sorted(Comparator.comparing(Group::firstLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(Group::secondLastName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(Group::firstName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        List<String> assigned = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            if (i == 0) {
                assigned.add(base);
            } else {
                assigned.add(base + (char) ('A' + i - 1));
            }
        }
        return assigned;
    }

    /**
     * Sort key + shared base for {@link #assignSuffixes(List)}.
     */
    public record Group(String baseGroup, String firstLastName, String secondLastName, String firstName) {
    }
}
