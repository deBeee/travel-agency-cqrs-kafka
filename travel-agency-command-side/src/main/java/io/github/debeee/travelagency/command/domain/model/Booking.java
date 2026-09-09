package io.github.debeee.travelagency.command.domain.model;

import java.time.LocalDate;

public record Booking(Long id, Long hotelId, Long userId, LocalDate start, LocalDate end) {

    public Booking {
        if (start == null || end == null) throw new IllegalArgumentException("Dates are required");
        if (start.isAfter(end)) throw new IllegalArgumentException("Start date cannot be after end date");
    }
}
