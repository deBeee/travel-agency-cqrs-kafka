package io.github.debeee.travelagency.command.application.port.out;

import io.github.debeee.travelagency.command.domain.model.Booking;

public interface BookingRepository {
    Booking save(Booking booking);
}
