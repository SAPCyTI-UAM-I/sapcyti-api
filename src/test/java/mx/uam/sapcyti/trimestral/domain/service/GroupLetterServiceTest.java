package mx.uam.sapcyti.trimestral.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for HU-57 group letter proposal rules
 * ({@code group_letter_assignment.feature}).
 */
class GroupLetterServiceTest {

    private final GroupLetterService service = new GroupLetterService();

    @ParameterizedTest(name = "n={0} → letter {1}, grupo {2}")
    @CsvSource({
        "1, O, CO43",
        "2, P, CP43",
        "3, Q, CQ43",
        "4, R, CR43",
        "5, S, CS43",
        "6, T, CT43",
        "7, U, CU43",
        "8, V, CV43",
        "9, W, CW43"
    })
    @DisplayName("HU-57: letter and base group for academic terms 1–9")
    void letterAndBaseGroupForTerms1To9(int n, char letter, String grupo) {
        Optional<Character> result = service.letterForTerm(n);

        assertThat(result).contains(letter);
        assertThat(service.baseGroup(result.orElseThrow())).isEqualTo(grupo);
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 11, 12, 0, -1})
    @DisplayName("HU-57: academic terms outside 1–9 have no letter")
    void noLetterOutsideRange(int n) {
        assertThat(service.letterForTerm(n)).isEmpty();
    }
}
