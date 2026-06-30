package com.certification.backend.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class BigDecimalUtilTest {

    @Test
    void formatRoundsHalfUpToFourDecimalPlaces() {
        assertThat(BigDecimalUtil.format(new BigDecimal("1.23445")))
                .isEqualByComparingTo("1.2345");
    }

    @Test
    void divideReturnsZeroForNullOrZeroDivisor() {
        assertThat(BigDecimalUtil.divide(new BigDecimal("10"), BigDecimal.ZERO))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(BigDecimalUtil.divide(null, new BigDecimal("2")))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void multiplyReturnsFourDecimalPlacesAndHandlesNull() {
        assertThat(BigDecimalUtil.multiply(new BigDecimal("1.2345"), new BigDecimal("2")))
                .isEqualByComparingTo("2.4690");
        assertThat(BigDecimalUtil.multiply(new BigDecimal("1"), null))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sumIgnoresNullValuesAndFormatsResult() {
        assertThat(BigDecimalUtil.sum(Arrays.asList(new BigDecimal("1.11111"), null, new BigDecimal("2.22222"))))
                .isEqualByComparingTo("3.3333");
        assertThat(BigDecimalUtil.sum(Collections.emptyList()))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void scoreToAchievementDividesByFullScore() {
        assertThat(BigDecimalUtil.scoreToAchievement(new BigDecimal("87.5")))
                .isEqualByComparingTo("0.8750");
        assertThat(BigDecimalUtil.scoreToAchievement(null))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
