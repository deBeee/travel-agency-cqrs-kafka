package io.github.debeee.travelagency.query.application.service;

import io.github.debeee.travelagency.query.application.port.out.AvailabilityReadRepository;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityWriteRepository;
import io.github.debeee.travelagency.query.application.port.out.HotelCapacityWriteRepository;
import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import io.github.debeee.travelagency.query.domain.policy.AvailabilityStatusPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class HotelCapacityServiceTest {

    private static final long HOTEL_ID = 7L;
    private static final LocalDate JUNE_1 = LocalDate.of(2027, 6, 1);
    private static final LocalDate JUNE_2 = LocalDate.of(2027, 6, 2);
    private static final double LAST_ROOMS_THRESHOLD = 0.9;

    @Mock
    private HotelCapacityWriteRepository hotelCapacityWriteRepository;

    @Mock
    private AvailabilityReadRepository availabilityReadRepository;

    @Mock
    private AvailabilityWriteRepository availabilityWriteRepository;

    private HotelCapacityService hotelCapacityService;

    @BeforeEach
    void setUp() {
        hotelCapacityService = new HotelCapacityService(
                hotelCapacityWriteRepository,
                availabilityReadRepository,
                availabilityWriteRepository,
                new AvailabilityStatusPolicy(LAST_ROOMS_THRESHOLD)
        );
    }

    @Test
    void shouldSaveCapacityThenReprojectEveryDayWhenCapacityGrows() {
        // given
        long newCapacity = 20;
        List<Availability> projectedDays = List.of(
                new Availability(HOTEL_ID, JUNE_1, 10, 10, AvailabilityStatus.SOLD_OUT),
                new Availability(HOTEL_ID, JUNE_2, 9, 10, AvailabilityStatus.LAST_ROOMS));
        given(availabilityReadRepository.findByHotel(HOTEL_ID, null, null)).willReturn(projectedDays);
        Availability expectedJune1 = new Availability(HOTEL_ID, JUNE_1, 10, 20, AvailabilityStatus.AVAILABLE);
        Availability expectedJune2 = new Availability(HOTEL_ID, JUNE_2, 9, 20, AvailabilityStatus.AVAILABLE);
        InOrder inOrder = inOrder(hotelCapacityWriteRepository, availabilityWriteRepository);

        // when
        hotelCapacityService.upsert(HOTEL_ID, newCapacity);

        // then
        then(hotelCapacityWriteRepository).should(inOrder).save(HOTEL_ID, newCapacity);
        then(availabilityWriteRepository).should(inOrder).upsert(expectedJune1);
        then(availabilityWriteRepository).should(inOrder).upsert(expectedJune2);
    }

    @Test
    void shouldReprojectDaysToSoldOutAndLastRoomsWhenCapacityShrinks() {
        // given
        long newCapacity = 10;
        List<Availability> projectedDays = List.of(
                new Availability(HOTEL_ID, JUNE_1, 10, 20, AvailabilityStatus.AVAILABLE),
                new Availability(HOTEL_ID, JUNE_2, 9, 20, AvailabilityStatus.AVAILABLE));
        given(availabilityReadRepository.findByHotel(HOTEL_ID, null, null)).willReturn(projectedDays);
        Availability expectedJune1 = new Availability(HOTEL_ID, JUNE_1, 10, 10, AvailabilityStatus.SOLD_OUT);
        Availability expectedJune2 = new Availability(HOTEL_ID, JUNE_2, 9, 10, AvailabilityStatus.LAST_ROOMS);

        // when
        hotelCapacityService.upsert(HOTEL_ID, newCapacity);

        // then
        then(availabilityWriteRepository).should().upsert(expectedJune1);
        then(availabilityWriteRepository).should().upsert(expectedJune2);
        then(availabilityWriteRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldOnlySaveCapacityWhenHotelHasNoProjectedDays() {
        // given
        long capacity = 10;
        given(availabilityReadRepository.findByHotel(HOTEL_ID, null, null)).willReturn(List.of());

        // when
        hotelCapacityService.upsert(HOTEL_ID, capacity);

        // then
        then(hotelCapacityWriteRepository).should().save(HOTEL_ID, capacity);
        then(availabilityWriteRepository).shouldHaveNoInteractions();
    }
}
