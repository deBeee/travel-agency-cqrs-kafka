package io.github.debeee.travelagency.query.presentation.mapper;

import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import io.github.debeee.travelagency.query.presentation.dto.AvailabilityResponseDto;
import io.github.debeee.travelagency.query.presentation.dto.AvailabilityStatusDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityResponseMapperTest {

    private static final long HOTEL_ID = 7L;
    private static final LocalDate JUNE_1 = LocalDate.of(2027, 6, 1);
    private static final LocalDate JUNE_2 = LocalDate.of(2027, 6, 2);
    private static final LocalDate JUNE_3 = LocalDate.of(2027, 6, 3);

    private final AvailabilityResponseMapper mapper = new AvailabilityResponseMapper();

    @Test
    void shouldMapEveryStatusAndComputeFreeRoomsWhenMappingAvailabilities() {
        // given
        List<Availability> availabilities = List.of(
                new Availability(HOTEL_ID, JUNE_1, 3, 10, AvailabilityStatus.AVAILABLE),
                new Availability(HOTEL_ID, JUNE_2, 9, 10, AvailabilityStatus.LAST_ROOMS),
                new Availability(HOTEL_ID, JUNE_3, 10, 10, AvailabilityStatus.SOLD_OUT));
        List<AvailabilityResponseDto> expectedDtos = List.of(
                new AvailabilityResponseDto(HOTEL_ID, JUNE_1, 3, 10, 7, AvailabilityStatusDto.AVAILABLE),
                new AvailabilityResponseDto(HOTEL_ID, JUNE_2, 9, 10, 1, AvailabilityStatusDto.LAST_ROOMS),
                new AvailabilityResponseDto(HOTEL_ID, JUNE_3, 10, 10, 0, AvailabilityStatusDto.SOLD_OUT));

        // when
        List<AvailabilityResponseDto> dtos = mapper.toAvailabilityResponseDtos(availabilities);

        // then
        assertThat(dtos).containsExactlyElementsOf(expectedDtos);
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoAvailabilities() {
        // given
        List<Availability> availabilities = List.of();

        // when
        List<AvailabilityResponseDto> dtos = mapper.toAvailabilityResponseDtos(availabilities);

        // then
        assertThat(dtos).isEmpty();
    }
}
