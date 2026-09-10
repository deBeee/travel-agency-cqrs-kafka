package io.github.debeee.travelagency.command.infrastructure.persistence.mapper;

import io.github.debeee.travelagency.command.domain.model.DailyAvailability;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.DailyAvailabilityEntity;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityMapper {

    public DailyAvailability toDailyAvailability(DailyAvailabilityEntity entity) {
        return new DailyAvailability(
                entity.getHotelId(),
                entity.getDate(),
                entity.getOccupiedRooms()
        );
    }

    public DailyAvailabilityEntity toDailyAvailabilityEntity(DailyAvailability domain) {
        return DailyAvailabilityEntity.builder()
                .hotelId(domain.getHotelId())
                .date(domain.getDate())
                .occupiedRooms(domain.getOccupiedRooms())
                .build();
    }
}
