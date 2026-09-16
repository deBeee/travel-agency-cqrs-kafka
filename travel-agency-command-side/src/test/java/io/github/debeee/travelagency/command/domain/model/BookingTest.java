package io.github.debeee.travelagency.command.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class BookingTest {

    private static final Long HOTEL_ID = 7L;
    private static final Long USER_ID = 100L;

    @Test
    void shouldCreateBookingWhenStartIsBeforeEnd() {
        // given
        LocalDate start = LocalDate.of(2027, 6, 1);
        LocalDate end = LocalDate.of(2027, 6, 3);

        // when
        Booking booking = new Booking(null, HOTEL_ID, USER_ID, start, end);

        // then
        assertAll(
                () -> assertThat(booking.id()).isNull(),
                () -> assertThat(booking.hotelId()).isEqualTo(HOTEL_ID),
                () -> assertThat(booking.userId()).isEqualTo(USER_ID),
                () -> assertThat(booking.start()).isEqualTo(start),
                () -> assertThat(booking.end()).isEqualTo(end)
        );
    }

    @Test
    void shouldCreateBookingWhenStartEqualsEnd() {
        // given
        LocalDate singleNight = LocalDate.of(2027, 6, 1);

        // when
        Booking booking = new Booking(1L, HOTEL_ID, USER_ID, singleNight, singleNight);

        // then
        assertAll(
                () -> assertThat(booking.start()).isEqualTo(singleNight),
                () -> assertThat(booking.end()).isEqualTo(singleNight)
        );
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenStartIsAfterEnd() {
        // given
        LocalDate start = LocalDate.of(2027, 6, 3);
        LocalDate end = LocalDate.of(2027, 6, 1);

        // when & then
        assertThatThrownBy(() -> new Booking(null, HOTEL_ID, USER_ID, start, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date cannot be after end date");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenStartIsNull() {
        // given
        LocalDate end = LocalDate.of(2027, 6, 1);

        // when & then
        assertThatThrownBy(() -> new Booking(null, HOTEL_ID, USER_ID, null, end))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dates are required");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEndIsNull() {
        // given
        LocalDate start = LocalDate.of(2027, 6, 1);

        // when & then
        assertThatThrownBy(() -> new Booking(null, HOTEL_ID, USER_ID, start, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Dates are required");
    }
}
