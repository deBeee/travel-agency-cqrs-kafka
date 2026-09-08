package io.github.debeee.travelagency.command.infrastructure.persistence.adapter;

import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.domain.model.Hotel;
import io.github.debeee.travelagency.command.infrastructure.persistence.mapper.TravelMapper;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaHotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TravelPersistenceAdapter implements HotelRepository {

    private final JpaHotelRepository jpaHotelRepository;
    private final TravelMapper travelMapper;

    @Override
    public Optional<Hotel> findHotel(Long id) {
        return jpaHotelRepository
                .findById(id)
                .map(travelMapper::toHotelDomain);
    }

    @Override
    public Hotel saveHotel(Hotel hotel) {
        var hotelEntity = travelMapper.toHotelEntity(hotel);
        var saved = jpaHotelRepository.save(hotelEntity);
        return travelMapper.toHotelDomain(saved);
    }
}
