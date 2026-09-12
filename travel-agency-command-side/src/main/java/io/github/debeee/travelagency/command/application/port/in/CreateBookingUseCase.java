package io.github.debeee.travelagency.command.application.port.in;

import io.github.debeee.travelagency.command.application.command.CreateBookingCommand;

public interface CreateBookingUseCase {
    Long createBooking(CreateBookingCommand command);
}
