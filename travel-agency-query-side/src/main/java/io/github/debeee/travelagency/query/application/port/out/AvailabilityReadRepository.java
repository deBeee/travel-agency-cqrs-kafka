package io.github.debeee.travelagency.query.application.port.out;

import io.github.debeee.travelagency.query.domain.model.Availability;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilityReadRepository {
    List<Availability> findByHotel(long hotelId, LocalDate from, LocalDate to);
}
