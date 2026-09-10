package io.github.debeee.travelagency.command.infrastructure.persistence.adapter;

import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.domain.model.Hotel;
import io.github.debeee.travelagency.command.infrastructure.persistence.mapper.HotelMapper;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaHotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HotelRepositoryAdapter implements HotelRepository {

    private final JpaHotelRepository jpaHotelRepository;
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
}
