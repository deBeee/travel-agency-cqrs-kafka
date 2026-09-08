package io.github.debeee.travelagency.command.presentation.dto;

import jakarta.validation.constraints.Positive;

public record CreateHotelRequestDto(@Positive long capacity) {
}
