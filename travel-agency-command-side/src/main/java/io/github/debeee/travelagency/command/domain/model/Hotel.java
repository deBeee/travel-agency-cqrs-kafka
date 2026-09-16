package io.github.debeee.travelagency.command.domain.model;

public record Hotel(Long id, long capacity) {

    public Hotel {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
    }
}
