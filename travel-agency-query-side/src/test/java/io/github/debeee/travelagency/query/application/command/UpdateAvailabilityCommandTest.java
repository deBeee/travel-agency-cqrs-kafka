package io.github.debeee.travelagency.query.application.command;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class UpdateAvailabilityCommandTest {

    @Test
    void shouldCreateCommandWhenDateIsPresent() {
        // given
        long hotelId = 7L;
        LocalDate date = LocalDate.of(2027, 6, 1);
        long occupied = 3;

        // when
        UpdateAvailabilityCommand command = new UpdateAvailabilityCommand(hotelId, date, occupied);

        // then
        assertAll(
                () -> assertThat(command.hotelId()).isEqualTo(hotelId),
                () -> assertThat(command.date()).isEqualTo(date),
                () -> assertThat(command.occupied()).isEqualTo(occupied)
        );
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenDateIsNull() {
        // given
        String expectedMessage = "Date is required";

        // when & then
        assertThatThrownBy(() -> new UpdateAvailabilityCommand(7L, null, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }
}
