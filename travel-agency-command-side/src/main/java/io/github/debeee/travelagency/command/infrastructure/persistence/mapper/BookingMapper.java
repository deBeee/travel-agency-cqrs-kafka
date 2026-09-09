package io.github.debeee.travelagency.command.infrastructure.persistence.mapper;

import io.github.debeee.travelagency.command.domain.model.Booking;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.BookingEntity;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingEntity toBookingEntity(Booking booking) {
        return BookingEntity.builder()
                .id(booking.id())
                .hotelId(booking.hotelId())
                .userId(booking.userId())
                .startDate(booking.start())
                .endDate(booking.end())
                .build();
    }

    public Booking toBookingDomain(BookingEntity entity) {
        return new Booking(
                entity.getId(),
                entity.getHotelId(),
                entity.getUserId(),
                entity.getStartDate(),
                entity.getEndDate()
        );
    }
}
