package io.github.debeee.travelagency.command.domain.model;

import io.github.debeee.travelagency.command.domain.exception.OverbookingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class DailyAvailabilityTest {

    private static final Long HOTEL_ID = 7L;
    private static final LocalDate DATE = LocalDate.of(2027, 6, 1);

    @Test
    void shouldExposeStateWhenCreated() {
        // given
        long occupiedRooms = 4;

        // when
        DailyAvailability availability = new DailyAvailability(HOTEL_ID, DATE, occupiedRooms);

        // then
        assertAll(
                () -> assertThat(availability.getHotelId()).isEqualTo(HOTEL_ID),
                () -> assertThat(availability.getDate()).isEqualTo(DATE),
                () -> assertThat(availability.getOccupiedRooms()).isEqualTo(occupiedRooms)
        );
    }

    @Test
    void shouldIncrementOccupiedRoomsWhenCapacityIsNotReached() {
        // given
        DailyAvailability availability = new DailyAvailability(HOTEL_ID, DATE, 0);

        // when
        availability.reserveOne(2);

        // then
        assertThat(availability.getOccupiedRooms()).isEqualTo(1);
    }

    @Test
    void shouldReserveLastRoomWhenOneRoomIsLeft() {
        // given
        DailyAvailability availability = new DailyAvailability(HOTEL_ID, DATE, 1);

        // when
        availability.reserveOne(2);

        // then
        assertThat(availability.getOccupiedRooms()).isEqualTo(2);
    }

    @Test
    void shouldAccumulateOccupiedRoomsWhenReserveOneIsCalledRepeatedly() {
        // given
        DailyAvailability availability = new DailyAvailability(HOTEL_ID, DATE, 0);

        // when
        availability.reserveOne(3);
        availability.reserveOne(3);
        availability.reserveOne(3);

        // then
        assertThat(availability.getOccupiedRooms()).isEqualTo(3);
    }

    @Test
    void shouldThrowOverbookingExceptionWhenOccupiedRoomsEqualCapacity() {
        // given
        DailyAvailability availability = new DailyAvailability(HOTEL_ID, DATE, 2);

        // when & then
        assertThatThrownBy(() -> availability.reserveOne(2))
                .isInstanceOf(OverbookingException.class)
                .hasMessage("Hotel 7 overbooked on 2027-06-01. Capacity: 2, occupied: 2");
    }

    @Test
    void shouldThrowOverbookingExceptionWhenOccupiedRoomsExceedCapacity() {
        // given
        DailyAvailability availability = new DailyAvailability(HOTEL_ID, DATE, 5);

        // when & then
        assertThatThrownBy(() -> availability.reserveOne(2))
                .isInstanceOf(OverbookingException.class);
    }

    @Test
    void shouldKeepOccupiedRoomsUnchangedWhenReservationIsRejected() {
        // given
        DailyAvailability availability = new DailyAvailability(HOTEL_ID, DATE, 2);

        // when & then
        assertThatThrownBy(() -> availability.reserveOne(2))
                .isInstanceOf(OverbookingException.class);
        assertThat(availability.getOccupiedRooms()).isEqualTo(2);
    }
}
