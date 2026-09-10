package io.github.debeee.travelagency.command.domain.model;

public class Hotel {
    private final Long id;
    private final long capacity;

    public Hotel(Long id, long capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.id = id;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public long getCapacity() {
        return capacity;
    }
}
