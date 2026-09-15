package io.github.debeee.travelagency.command.application.service;

import io.github.debeee.travelagency.command.application.exception.HotelNotFoundException;
import io.github.debeee.travelagency.command.application.port.in.CreateHotelUseCase;
import io.github.debeee.travelagency.command.application.port.in.UpdateHotelCapacityUseCase;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.application.port.out.OutboxRepository;
import io.github.debeee.travelagency.command.domain.model.Hotel;

public class HotelService implements CreateHotelUseCase, UpdateHotelCapacityUseCase {

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

    @Override
    public void updateCapacity(Long hotelId, long capacity) {
        Hotel existing = hotelRepository.findHotel(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        Hotel updated = hotelRepository.saveHotel(new Hotel(existing.getId(), capacity));
        hotelOutboxRepository.saveOutbox(updated);
    }
}
