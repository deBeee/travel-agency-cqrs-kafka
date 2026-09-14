package io.github.debeee.travelagency.query.presentation.dto;

import java.time.LocalDate;

public record AvailabilityResponseDto(
        long hotelId,
        LocalDate date,
        long occupied,
        long capacity,
        long freeRooms,
        AvailabilityStatusDto status
) {
}
