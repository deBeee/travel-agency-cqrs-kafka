package io.github.debeee.travelagency.query.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class AvailabilityTest {

    private static final long HOTEL_ID = 7L;
    private static final LocalDate DATE = LocalDate.of(2027, 6, 1);

    @Test
    void shouldExposeStateWhenCreated() {
        // given
        long occupied = 3;
        long capacity = 10;

        // when
        Availability availability = new Availability(HOTEL_ID, DATE, occupied, capacity, AvailabilityStatus.AVAILABLE);

        // then
        assertAll(
                () -> assertThat(availability.hotelId()).isEqualTo(HOTEL_ID),
                () -> assertThat(availability.date()).isEqualTo(DATE),
                () -> assertThat(availability.occupied()).isEqualTo(occupied),
                () -> assertThat(availability.capacity()).isEqualTo(capacity),
                () -> assertThat(availability.status()).isEqualTo(AvailabilityStatus.AVAILABLE)
        );
    }

    @ParameterizedTest
    @CsvSource({
            " 0, 10, 10",
            " 3, 10,  7",
            "10, 10,  0",
            "12, 10,  0",
    })
    void shouldCalculateFreeRoomsWhenOccupancyIsKnown(long occupied, long capacity, long expectedFreeRooms) {
        // given
        Availability availability = new Availability(HOTEL_ID, DATE, occupied, capacity, AvailabilityStatus.AVAILABLE);

        // when
        long freeRooms = availability.freeRooms();

        // then
        assertThat(freeRooms).isEqualTo(expectedFreeRooms);
    }

    @Test
    void shouldReturnCopyWithNewCapacityAndStatusWhenCapacityChanges() {
        // given
        Availability availability = new Availability(HOTEL_ID, DATE, 10, 10, AvailabilityStatus.SOLD_OUT);
        Availability expectedAvailability = new Availability(HOTEL_ID, DATE, 10, 20, AvailabilityStatus.AVAILABLE);

        // when
        Availability corrected = availability.withCapacityAndStatus(20, AvailabilityStatus.AVAILABLE);

        // then
        assertThat(corrected).isEqualTo(expectedAvailability);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenDateIsNull() {
        // given
        String expectedMessage = "Date is required";

        // when & then
        assertThatThrownBy(() -> new Availability(HOTEL_ID, null, 0, 10, AvailabilityStatus.AVAILABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenOccupiedIsNegative() {
        // given
        String expectedMessage = "Occupied cannot be negative";

        // when & then
        assertThatThrownBy(() -> new Availability(HOTEL_ID, DATE, -1, 10, AvailabilityStatus.AVAILABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @ParameterizedTest
    @CsvSource({"0", "-1"})
    void shouldThrowIllegalArgumentExceptionWhenCapacityIsNotPositive(long capacity) {
        // given
        String expectedMessage = "Capacity must be positive";

        // when & then
        assertThatThrownBy(() -> new Availability(HOTEL_ID, DATE, 0, capacity, AvailabilityStatus.AVAILABLE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenStatusIsNull() {
        // given
        String expectedMessage = "Status is required";

        // when & then
        assertThatThrownBy(() -> new Availability(HOTEL_ID, DATE, 0, 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }
}
