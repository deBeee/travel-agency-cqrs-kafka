package io.github.debeee.travelagency.command.application.service;

import io.github.debeee.travelagency.command.application.port.in.CreateHotelUseCase;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.application.port.out.OutboxRepository;
import io.github.debeee.travelagency.command.domain.model.Hotel;

public class HotelService implements CreateHotelUseCase {

    private final HotelRepository hotelRepository;
    private final OutboxRepository<Hotel> hotelOutboxRepository;

    public HotelService(HotelRepository hotelRepository, OutboxRepository<Hotel> hotelOutboxRepository) {
        this.hotelRepository = hotelRepository;
        this.hotelOutboxRepository = hotelOutboxRepository;
    }

    @Override
    public Long createHotel(long capacity) {
        Hotel saved = hotelRepository.saveHotel(new Hotel(null, capacity));
        hotelOutboxRepository.saveOutbox(saved);
        return saved.getId();
    }
}
