package io.github.debeee.travelagency.query.presentation.mapper;

import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import io.github.debeee.travelagency.query.presentation.dto.AvailabilityResponseDto;
import io.github.debeee.travelagency.query.presentation.dto.AvailabilityStatusDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AvailabilityResponseMapper {

    public List<AvailabilityResponseDto> toAvailabilityResponseDtos(List<Availability> availabilities) {
        return availabilities.stream()
                .map(a -> new AvailabilityResponseDto(
                        a.hotelId(),
                        a.date(),
                        a.occupied(),
                        a.capacity(),
                        a.freeRooms(),
                        toAvailabilityStatusDto(a.status())
                ))
                .toList();
    }

    private AvailabilityStatusDto toAvailabilityStatusDto(AvailabilityStatus status) {
        return switch (status) {
            case AVAILABLE -> AvailabilityStatusDto.AVAILABLE;
            case LAST_ROOMS -> AvailabilityStatusDto.LAST_ROOMS;
            case SOLD_OUT -> AvailabilityStatusDto.SOLD_OUT;
        };
    }
}
