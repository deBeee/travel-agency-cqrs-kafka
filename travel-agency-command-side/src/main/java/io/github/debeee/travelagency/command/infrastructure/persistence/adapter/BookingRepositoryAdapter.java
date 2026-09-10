package io.github.debeee.travelagency.command.infrastructure.persistence.adapter;

import io.github.debeee.travelagency.command.application.port.out.BookingRepository;
import io.github.debeee.travelagency.command.domain.model.Booking;
import io.github.debeee.travelagency.command.infrastructure.persistence.mapper.BookingMapper;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingRepositoryAdapter implements BookingRepository {

    private final JpaBookingRepository jpaBookingRepository;
    private final BookingMapper bookingMapper;

    @Override
    public Booking save(Booking booking) {
        var entity = bookingMapper.toBookingEntity(booking);
        var saved = jpaBookingRepository.save(entity);
        return bookingMapper.toBookingDomain(saved);
    }
}
