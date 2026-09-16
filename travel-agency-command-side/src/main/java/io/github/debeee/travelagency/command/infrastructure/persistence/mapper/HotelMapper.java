package io.github.debeee.travelagency.command.infrastructure.persistence.mapper;

import io.github.debeee.travelagency.command.domain.model.Hotel;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.HotelEntity;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {
    
    public Hotel toHotelDomain(HotelEntity entity) {
        return new Hotel(
                entity.getId(),
                entity.getCapacity()
        );
    }

    public HotelEntity toHotelEntity(Hotel hotel) {
        return HotelEntity.builder()
                .id(hotel.id())
                .capacity(hotel.capacity())
                .build();
    }
}
