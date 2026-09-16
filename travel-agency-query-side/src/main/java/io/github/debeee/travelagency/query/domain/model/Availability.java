package io.github.debeee.travelagency.query.domain.model;

import java.time.LocalDate;

public record Availability(
        long hotelId,
        LocalDate date,
        long occupied,
        long capacity,
        AvailabilityStatus status) {

    public Availability {
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }

        if (occupied < 0) {
            throw new IllegalArgumentException("Occupied cannot be negative");
        }

        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }

        if (status == null) {
            throw new IllegalArgumentException("Status is required");
        }
    }

    public long freeRooms() {
        return Math.max(0L, capacity - occupied);
    }

    public Availability withCapacityAndStatus(long capacity, AvailabilityStatus status) {
        return new Availability(hotelId, date, occupied, capacity, status);
    }
}
