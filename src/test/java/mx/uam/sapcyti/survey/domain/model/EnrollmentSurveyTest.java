package mx.uam.sapcyti.survey.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnrollmentSurveyTest {

    @Test
    @DisplayName("status is PROGRAMADO before opensAt")
    void programadoBeforeOpen() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        EnrollmentSurvey survey = sampleSurvey(
                now.plusSeconds(3600),
                now.plusSeconds(7200));

        assertThat(survey.getStatus(now)).isEqualTo(SurveyStatus.PROGRAMADO);
    }

    @Test
    @DisplayName("status is ACTIVO within window")
    void activoWithinWindow() {
        Instant opens = Instant.parse("2026-01-01T00:00:00Z");
        Instant closes = Instant.parse("2026-02-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-15T00:00:00Z");
        EnrollmentSurvey survey = sampleSurvey(opens, closes);

        assertThat(survey.getStatus(now)).isEqualTo(SurveyStatus.ACTIVO);
    }

    @Test
    @DisplayName("status is CERRADO after closesAt")
    void cerradoAfterClose() {
        Instant opens = Instant.parse("2026-01-01T00:00:00Z");
        Instant closes = Instant.parse("2026-02-01T00:00:00Z");
        Instant now = Instant.parse("2026-03-01T00:00:00Z");
        EnrollmentSurvey survey = sampleSurvey(opens, closes);

        assertThat(survey.getStatus(now)).isEqualTo(SurveyStatus.CERRADO);
    }

    @Test
    @DisplayName("manual close forces CERRADO even inside window")
    void cerradoWhenClosedManually() {
        Instant opens = Instant.parse("2026-01-01T00:00:00Z");
        Instant closes = Instant.parse("2026-02-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-15T00:00:00Z");
        EnrollmentSurvey survey = sampleSurvey(opens, closes);
        survey.closeManually();

        assertThat(survey.getStatus(now)).isEqualTo(SurveyStatus.CERRADO);
    }

    private static EnrollmentSurvey sampleSurvey(Instant opensAt, Instant closesAt) {
        return EnrollmentSurvey.create(1L, "26O", opensAt, closesAt, null, 1L, List.of(10L));
    }
}
