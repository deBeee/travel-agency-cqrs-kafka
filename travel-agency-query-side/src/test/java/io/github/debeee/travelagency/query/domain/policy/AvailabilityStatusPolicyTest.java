package io.github.debeee.travelagency.query.domain.policy;

import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvailabilityStatusPolicyTest {

    @ParameterizedTest
    @CsvSource({
            "0.9,  0, 10, AVAILABLE",
            "0.9,  8, 10, AVAILABLE",
            "0.9,  9, 10, LAST_ROOMS",
            "0.9, 10, 10, SOLD_OUT",
            "0.9, 11, 10, SOLD_OUT",
            "0.5,  1,  2, LAST_ROOMS",
            "0.5,  4, 10, AVAILABLE",
            "0.5,  5, 10, LAST_ROOMS",
    })
    void shouldEvaluateStatusWhenOccupancyIsComparedWithThreshold(
            double threshold, long occupied, long capacity, AvailabilityStatus expectedStatus) {
        // given
        AvailabilityStatusPolicy policy = new AvailabilityStatusPolicy(threshold);

        // when
        AvailabilityStatus status = policy.evaluate(occupied, capacity);

        // then
        assertThat(status).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "0, AVAILABLE",
            "1, SOLD_OUT",
    })
    void shouldNeverReportLastRoomsWhenHotelHasSingleRoom(long occupied, AvailabilityStatus expectedStatus) {
        // given
        AvailabilityStatusPolicy policy = new AvailabilityStatusPolicy(0.9);
        long capacity = 1;

        // when
        AvailabilityStatus status = policy.evaluate(occupied, capacity);

        // then
        assertThat(status).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            " 9, AVAILABLE",
            "10, SOLD_OUT",
    })
    void shouldSkipLastRoomsWhenThresholdIsOne(long occupied, AvailabilityStatus expectedStatus) {
        // given
        AvailabilityStatusPolicy policy = new AvailabilityStatusPolicy(1.0);
        long capacity = 10;

        // when
        AvailabilityStatus status = policy.evaluate(occupied, capacity);

        // then
        assertThat(status).isEqualTo(expectedStatus);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 0.5, 1.0})
    void shouldCreatePolicyWhenThresholdIsWithinRange(double threshold) {
        // when & then
        assertThatCode(() -> new AvailabilityStatusPolicy(threshold)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.1, 1.01, 2.0})
    void shouldThrowIllegalArgumentExceptionWhenThresholdIsOutsideRange(double threshold) {
        // given
        String expectedMessage = "lastRoomsThreshold must be in (0, 1]";

        // when & then
        assertThatThrownBy(() -> new AvailabilityStatusPolicy(threshold))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }
}
