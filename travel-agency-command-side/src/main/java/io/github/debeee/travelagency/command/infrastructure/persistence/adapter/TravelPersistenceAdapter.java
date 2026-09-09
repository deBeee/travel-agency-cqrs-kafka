package io.github.debeee.travelagency.command.infrastructure.persistence.adapter;

import io.github.debeee.travelagency.command.application.port.out.BookingRepository;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.domain.model.Booking;
import io.github.debeee.travelagency.command.domain.model.Hotel;
import io.github.debeee.travelagency.command.infrastructure.persistence.mapper.BookingMapper;
import io.github.debeee.travelagency.command.infrastructure.persistence.mapper.HotelMapper;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaBookingRepository;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaHotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TravelPersistenceAdapter implements HotelRepository, BookingRepository {

    private final JpaHotelRepository jpaHotelRepository;
    private final JpaBookingRepository jpaBookingRepository;
    private final BookingMapper bookingMapper;
    private final HotelMapper hotelMapper;

    @Override
    public Optional<Hotel> findHotel(Long id) {
        return jpaHotelRepository
                .findById(id)
                .map(hotelMapper::toHotelDomain);
    }

    @Override
    public Hotel saveHotel(Hotel hotel) {
        var hotelEntity = hotelMapper.toHotelEntity(hotel);
        var saved = jpaHotelRepository.save(hotelEntity);
        return hotelMapper.toHotelDomain(saved);
    }

    @Override
    public Booking save(Booking booking) {
        var entity = bookingMapper.toBookingEntity(booking);
        var saved = jpaBookingRepository.save(entity);
        return bookingMapper.toBookingDomain(saved);
    }
}
