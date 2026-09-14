package io.github.debeee.travelagency.query.domain.model;

import java.time.LocalDate;

public class Availability {
    private final long hotelId;
    private final LocalDate date;
    private final long occupied;
    private final long capacity;
    private final AvailabilityStatus status;

    public Availability(long hotelId, LocalDate date, long occupied, long capacity, AvailabilityStatus status) {

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

        this.hotelId = hotelId;
        this.date = date;
        this.occupied = occupied;
        this.capacity = capacity;
        this.status = status;
    }

    public long freeRooms() {
        return Math.max(0L, capacity - occupied);
    }

    public long getHotelId() {
        return hotelId;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getOccupied() {
        return occupied;
    }

    public long getCapacity() {
        return capacity;
    }

    public AvailabilityStatus getStatus() {
        return status;
    }
}
