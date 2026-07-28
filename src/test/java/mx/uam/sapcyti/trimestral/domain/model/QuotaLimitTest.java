package mx.uam.sapcyti.trimestral.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuotaLimitTest {

    @Test
    void returnsFiniteValueForPositiveInteger() {
        assertThat(QuotaLimit.finiteValue("15")).isEqualTo(15);
    }

    @Test
    void returnsNullForUnlimitedOrAbsentQuota() {
        assertThat(QuotaLimit.finiteValue("*")).isNull();
        assertThat(QuotaLimit.finiteValue(null)).isNull();
        assertThat(QuotaLimit.finiteValue("")).isNull();
    }
}
