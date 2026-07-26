package mx.uam.sapcyti.trimestral.domain.service;

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

}
