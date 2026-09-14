package io.github.debeee.travelagency.query.application.service;

import io.github.debeee.travelagency.query.application.command.UpdateAvailabilityCommand;
import io.github.debeee.travelagency.query.application.port.in.GetAvailabilityUseCase;
import io.github.debeee.travelagency.query.application.port.in.UpdateAvailabilityUseCase;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityReadRepository;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityWriteRepository;
import io.github.debeee.travelagency.query.application.port.out.HotelCapacityProvider;
import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import io.github.debeee.travelagency.query.domain.policy.AvailabilityStatusPolicy;

import java.time.LocalDate;
import java.util.List;

public class AvailabilityService implements GetAvailabilityUseCase, UpdateAvailabilityUseCase {

    private final AvailabilityReadRepository availabilityReadRepository;
    private final AvailabilityWriteRepository availabilityWriteRepository;
    private final AvailabilityStatusPolicy availabilityStatusPolicy;
    private final HotelCapacityProvider hotelCapacityProvider;

    public AvailabilityService(AvailabilityReadRepository availabilityReadRepository,
                               AvailabilityWriteRepository availabilityWriteRepository,
                               AvailabilityStatusPolicy availabilityStatusPolicy,
                               HotelCapacityProvider hotelCapacityProvider) {
        this.availabilityReadRepository = availabilityReadRepository;
        this.availabilityWriteRepository = availabilityWriteRepository;
        this.availabilityStatusPolicy = availabilityStatusPolicy;
        this.hotelCapacityProvider = hotelCapacityProvider;
    }

    @Override
    public void update(UpdateAvailabilityCommand command) {
        long capacity = hotelCapacityProvider.getCapacity(command.hotelId());

        AvailabilityStatus status = availabilityStatusPolicy.evaluate(command.occupied(), capacity);

        Availability availability = new Availability(
                command.hotelId(),
                command.date(),
                command.occupied(),
                capacity,
                status
        );

        availabilityWriteRepository.upsert(availability);
    }

    @Override
    public List<Availability> getForHotel(long hotelId, LocalDate from, LocalDate to) {
        return availabilityReadRepository.findByHotel(hotelId, from, to);
    }
}
