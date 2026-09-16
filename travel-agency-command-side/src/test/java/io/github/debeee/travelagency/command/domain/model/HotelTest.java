package io.github.debeee.travelagency.command.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class HotelTest {

    @Test
    void shouldCreateHotelWhenCapacityIsPositive() {
        // given
        Long id = 1L;
        long capacity = 10;

        // when
        Hotel hotel = new Hotel(id, capacity);

        // then
        assertAll(
                () -> assertThat(hotel.id()).isEqualTo(id),
                () -> assertThat(hotel.capacity()).isEqualTo(capacity)
        );
    }

    @Test
    void shouldCreateHotelWhenIdIsNull() {
        // given
        long capacity = 1;

        // when
        Hotel hotel = new Hotel(null, capacity);

        // then
        assertAll(
                () -> assertThat(hotel.id()).isNull(),
                () -> assertThat(hotel.capacity()).isEqualTo(capacity)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void shouldThrowIllegalArgumentExceptionWhenCapacityIsNotPositive(long capacity) {
        // given
        Long id = 1L;

        // when & then
        assertThatThrownBy(() -> new Hotel(id, capacity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Capacity must be positive");
    }
}
