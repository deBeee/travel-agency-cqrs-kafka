package io.github.debeee.travelagency.command.application.service;

import io.github.debeee.travelagency.command.application.port.in.CreateHotelUseCase;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.domain.model.Hotel;

public class HotelService implements CreateHotelUseCase {

    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @Override
    public Long createHotel(long capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }

        Hotel saved = hotelRepository.saveHotel(new Hotel(null, capacity));
        return saved.getId();
    }
}
