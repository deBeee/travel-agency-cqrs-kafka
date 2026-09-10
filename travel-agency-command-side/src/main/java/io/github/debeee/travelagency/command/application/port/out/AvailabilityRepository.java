package io.github.debeee.travelagency.command.application.port.out;

import java.time.LocalDate;

public interface AvailabilityRepository {
    void reserveAvailability(Long hotelId, long capacity, LocalDate start, LocalDate end);
}
