package com.vita.aichat.chat.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextTokenEstimatorTest {

    @Test
    void shouldEstimateChineseTextHigherThanBlank() {
        ContextTokenEstimator estimator = new ContextTokenEstimator();

        assertThat(estimator.estimateText("")).isZero();
        assertThat(estimator.estimateText("今日苏州天气？")).isGreaterThan(0);
        assertThat(estimator.estimateText("今日苏州天气？"))
                .isGreaterThan(estimator.estimateText("weather"));
    }
}
