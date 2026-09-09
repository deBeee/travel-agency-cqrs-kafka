package io.github.debeee.travelagency.command.application.command;

import java.time.LocalDate;

public record CreateBookingCommand(
        Long hotelId,
        Long userId,
        LocalDate start,
        LocalDate end
) {
}
