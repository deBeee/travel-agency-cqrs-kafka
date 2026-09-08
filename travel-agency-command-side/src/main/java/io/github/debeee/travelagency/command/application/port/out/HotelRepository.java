package io.github.debeee.travelagency.command.application.port.out;

import io.github.debeee.travelagency.command.domain.model.Hotel;

import java.util.Optional;

public interface HotelRepository {

    Optional<Hotel> findHotel(Long id);
    Hotel saveHotel(Hotel hotel);
}
