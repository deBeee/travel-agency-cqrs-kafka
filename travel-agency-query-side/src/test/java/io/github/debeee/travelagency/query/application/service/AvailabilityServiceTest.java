package io.github.debeee.travelagency.query.application.service;

import io.github.debeee.travelagency.query.application.command.UpdateAvailabilityCommand;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityReadRepository;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityWriteRepository;
import io.github.debeee.travelagency.query.application.port.out.HotelCapacityProvider;
import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import io.github.debeee.travelagency.query.domain.policy.AvailabilityStatusPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    private static final long HOTEL_ID = 7L;
    private static final LocalDate DATE = LocalDate.of(2027, 6, 1);
    private static final double LAST_ROOMS_THRESHOLD = 0.9;

    @Mock
    private AvailabilityReadRepository availabilityReadRepository;

    @Mock
    private AvailabilityWriteRepository availabilityWriteRepository;

    @Mock
    private HotelCapacityProvider hotelCapacityProvider;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(
                availabilityReadRepository,
                availabilityWriteRepository,
                new AvailabilityStatusPolicy(LAST_ROOMS_THRESHOLD),
                hotelCapacityProvider
        );
    }

    @Test
    void shouldUpsertAvailabilityWithProvidedCapacityAndEvaluatedStatusWhenOccupancyChanges() {
        // given
        UpdateAvailabilityCommand command = new UpdateAvailabilityCommand(HOTEL_ID, DATE, 9);
        given(hotelCapacityProvider.getCapacity(HOTEL_ID)).willReturn(10L);
        Availability expectedAvailability = new Availability(HOTEL_ID, DATE, 9, 10, AvailabilityStatus.LAST_ROOMS);

        // when
        availabilityService.update(command);

        // then
        then(availabilityWriteRepository).should().upsert(expectedAvailability);
    }

    @Test
    void shouldUpsertSoldOutAvailabilityWhenOccupancyReachesCapacity() {
        // given
        UpdateAvailabilityCommand command = new UpdateAvailabilityCommand(HOTEL_ID, DATE, 2);
        given(hotelCapacityProvider.getCapacity(HOTEL_ID)).willReturn(2L);
        Availability expectedAvailability = new Availability(HOTEL_ID, DATE, 2, 2, AvailabilityStatus.SOLD_OUT);

        // when
        availabilityService.update(command);

        // then
        then(availabilityWriteRepository).should().upsert(expectedAvailability);
    }

    @Test
    void shouldReturnAvailabilitiesFromReadRepositoryWhenQueriedWithDateRange() {
        // given
        LocalDate from = DATE;
        LocalDate to = DATE.plusDays(2);
        List<Availability> expectedAvailabilities = List.of(
                new Availability(HOTEL_ID, DATE, 1, 10, AvailabilityStatus.AVAILABLE),
                new Availability(HOTEL_ID, DATE.plusDays(1), 10, 10, AvailabilityStatus.SOLD_OUT));
        given(availabilityReadRepository.findByHotel(HOTEL_ID, from, to)).willReturn(expectedAvailabilities);

        // when
        List<Availability> availabilities = availabilityService.getForHotel(HOTEL_ID, from, to);

        // then
        assertThat(availabilities).isEqualTo(expectedAvailabilities);
    }

    @Test
    void shouldPassNullRangeToReadRepositoryWhenQueriedWithoutDates() {
        // given
        List<Availability> expectedAvailabilities = List.of(
                new Availability(HOTEL_ID, DATE, 1, 10, AvailabilityStatus.AVAILABLE));
        given(availabilityReadRepository.findByHotel(HOTEL_ID, null, null)).willReturn(expectedAvailabilities);

        // when
        List<Availability> availabilities = availabilityService.getForHotel(HOTEL_ID, null, null);

        // then
        assertThat(availabilities).isEqualTo(expectedAvailabilities);
    }
}
