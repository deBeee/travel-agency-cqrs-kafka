package io.github.debeee.travelagency.command.domain.model;

import io.github.debeee.travelagency.command.domain.exception.OverbookingException;

import java.time.LocalDate;

public class DailyAvailability {
    private final Long hotelId;
    private final LocalDate date;
    private long occupiedRooms;

    public DailyAvailability(Long hotelId, LocalDate date, long occupiedRooms) {
        this.hotelId = hotelId;
        this.date = date;
        this.occupiedRooms = occupiedRooms;
    }

    public void reserveOne(long capacity) {
        if (occupiedRooms >= capacity) {
            throw new OverbookingException("Hotel %d overbooked on %s. Capacity: %d, occupied: %d".formatted(
                    hotelId, date, capacity, occupiedRooms
            ));
        }
        ++occupiedRooms;
    }

    public Long getHotelId() {
        return hotelId;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getOccupiedRooms() {
        return occupiedRooms;
    }
}
