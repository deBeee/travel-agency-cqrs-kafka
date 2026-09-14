package io.github.debeee.travelagency.query.application.command;

import java.time.LocalDate;

public record UpdateAvailabilityCommand(long hotelId, LocalDate date, long occupied) {
    public UpdateAvailabilityCommand {
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
    }
}
