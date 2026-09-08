package io.github.debeee.travelagency.command.domain.model;

public class Hotel {
    private final Long id;
    private final long capacity;

    public Hotel(Long id, long capacity) {
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
