package io.github.debeee.travelagency.query.application.service;

import io.github.debeee.travelagency.query.application.port.in.UpsertHotelCapacityUseCase;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityReadRepository;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityWriteRepository;
import io.github.debeee.travelagency.query.application.port.out.HotelCapacityWriteRepository;
import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import io.github.debeee.travelagency.query.domain.policy.AvailabilityStatusPolicy;

import java.util.List;

public class HotelCapacityService implements UpsertHotelCapacityUseCase {

    private final HotelCapacityWriteRepository hotelCapacityWriteRepository;
    private final AvailabilityReadRepository availabilityReadRepository;
    private final AvailabilityWriteRepository availabilityWriteRepository;
    private final AvailabilityStatusPolicy availabilityStatusPolicy;
    
    public HotelCapacityService(HotelCapacityWriteRepository hotelCapacityWriteRepository,
                                AvailabilityReadRepository availabilityReadRepository,
                                AvailabilityWriteRepository availabilityWriteRepository,
                                AvailabilityStatusPolicy availabilityStatusPolicy) {
        this.hotelCapacityWriteRepository = hotelCapacityWriteRepository;
        this.availabilityReadRepository = availabilityReadRepository;
        this.availabilityWriteRepository = availabilityWriteRepository;
        this.availabilityStatusPolicy = availabilityStatusPolicy;
    }

    @Override
    public void upsert(long hotelId, long capacity) {
        hotelCapacityWriteRepository.save(hotelId, capacity);

        reprojectHotelDays(hotelId, capacity);
    }

    private void reprojectHotelDays(long hotelId, long capacity) {
        List<Availability> availabilities = availabilityReadRepository.findByHotel(hotelId, null, null);

        for (Availability av : availabilities) {
            AvailabilityStatus newStatus = availabilityStatusPolicy.evaluate(av.getOccupied(), capacity);
            Availability corrected = av.withCapacityAndStatus(capacity, newStatus);
            availabilityWriteRepository.upsert(corrected);
        }
    }
}
