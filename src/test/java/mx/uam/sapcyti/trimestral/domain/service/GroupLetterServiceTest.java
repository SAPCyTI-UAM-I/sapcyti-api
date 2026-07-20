package mx.uam.sapcyti.trimestral.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import mx.uam.sapcyti.trimestral.domain.service.GroupLetterService.Group;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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

    @Test
    @DisplayName("HU-57: same UEA with cupo 1 gets surname-ordered suffixes")
    void assignSuffixesBySurname() {
        List<Group> groups = List.of(
                new Group("CR43", "Ramos", "Garcia", "Ana"),
                new Group("CR43", "Aguirre", "Perez", "Luis"),
                new Group("CR43", "Lopez", "Martinez", "Maria"));

        List<String> assigned = service.assignSuffixes(groups);

        assertThat(assigned).containsExactly("CR43", "CR43A", "CR43B");
    }

    @Test
    @DisplayName("HU-57: null secondLastName uses nullsLast and does not throw")
    void nullSecondLastNameSortsLast() {
        List<Group> groups = List.of(
                new Group("CR43", "Lopez", null, "Ana"),
                new Group("CR43", "Lopez", "Zeta", "Bruno"));

        assertThatCode(() -> service.assignSuffixes(groups)).doesNotThrowAnyException();

        List<String> assigned = service.assignSuffixes(groups);
        assertThat(assigned).containsExactly("CR43", "CR43A");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("emptyAssignCases")
    @DisplayName("HU-57: assignSuffixes handles empty/null input")
    void assignSuffixesEmpty(String label, List<Group> input) {
        assertThat(service.assignSuffixes(input)).isEmpty();
    }

    private static Stream<Arguments> emptyAssignCases() {
        return Stream.of(
                Arguments.of("null list", null),
                Arguments.of("empty list", List.of()));
    }
}
