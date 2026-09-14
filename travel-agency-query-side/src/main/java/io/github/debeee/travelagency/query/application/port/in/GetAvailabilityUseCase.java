package io.github.debeee.travelagency.query.application.port.in;

import io.github.debeee.travelagency.query.domain.model.Availability;

import java.time.LocalDate;
import java.util.List;

public interface GetAvailabilityUseCase {
    List<Availability> getForHotel(long hotelId, LocalDate from, LocalDate to);
}
