package io.github.debeee.travelagency.command.presentation.dto.hotel;

import jakarta.validation.constraints.Positive;

public record UpdateHotelCapacityRequestDto(@Positive(message = "Capacity must be positive") long capacity) {
}
