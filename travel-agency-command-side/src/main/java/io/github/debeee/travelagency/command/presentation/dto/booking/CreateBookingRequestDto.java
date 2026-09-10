package io.github.debeee.travelagency.command.presentation.dto.booking;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateBookingRequestDto(
        @NotNull(message = "Hotel id is required") Long hotelId,
        @NotNull(message = "User id is required") Long userId,
        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date must not be in the past") LocalDate start,
        @NotNull(message = "End date is required")
        @FutureOrPresent(message = "End date must not be in the past") LocalDate end
) {
}
