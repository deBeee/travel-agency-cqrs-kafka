package io.github.debeee.travelagency.command.application.event;

import java.time.LocalDate;

public record BookingCreatedPayload(
        Long id,
        Long hotelId,
        Long userId,
        LocalDate start,
        LocalDate end
) implements OutboxPayload {
}
