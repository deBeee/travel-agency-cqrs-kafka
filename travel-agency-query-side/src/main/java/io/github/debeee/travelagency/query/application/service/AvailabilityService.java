package io.github.debeee.travelagency.query.application.service;

import io.github.debeee.travelagency.query.application.port.in.GetAvailabilityUseCase;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityReadRepository;
import io.github.debeee.travelagency.query.domain.model.Availability;

import java.time.LocalDate;
import java.util.List;

public class AvailabilityService implements GetAvailabilityUseCase {

    private final AvailabilityReadRepository availabilityReadRepository;

    public AvailabilityService(AvailabilityReadRepository availabilityReadRepository) {
        this.availabilityReadRepository = availabilityReadRepository;
    }

    @Override
    public List<Availability> getForHotel(long hotelId, LocalDate from, LocalDate to) {
        return availabilityReadRepository.findByHotel(hotelId, from, to);
    }
}
