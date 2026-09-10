package io.github.debeee.travelagency.command.infrastructure.persistence.adapter;

import io.github.debeee.travelagency.command.application.port.out.AvailabilityRepository;
import io.github.debeee.travelagency.command.domain.model.DailyAvailability;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.DailyAvailabilityEntity;
import io.github.debeee.travelagency.command.infrastructure.persistence.mapper.AvailabilityMapper;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaDailyAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AvailabilityRepositoryAdapter implements AvailabilityRepository {

    private final JpaDailyAvailabilityRepository jpaDailyAvailabilityRepository;
    private final AvailabilityMapper availabilityMapper;

    @Override
    public void reserveAvailability(Long hotelId, long capacity, LocalDate start, LocalDate end) {
        Map<LocalDate, DailyAvailabilityEntity> existingSlots = jpaDailyAvailabilityRepository
                .findAndLockByHotelAndDateRange(hotelId, start, end)
                .stream()
                .collect(Collectors.toMap(DailyAvailabilityEntity::getDate, Function.identity()));

        List<DailyAvailabilityEntity> toSave = start
                .datesUntil(end.plusDays(1))
                .map(date -> reserveSlot(existingSlots, hotelId, date, capacity))
                .toList();

        jpaDailyAvailabilityRepository.saveAll(toSave);
    }

    private DailyAvailabilityEntity reserveSlot(
            Map<LocalDate, DailyAvailabilityEntity> existingSlots,
            Long hotelId,
            LocalDate date,
            long capacity
    ) {
        DailyAvailabilityEntity existing = existingSlots.get(date);
        DailyAvailability availability = existing != null
                ? availabilityMapper.toDailyAvailability(existing)
                : new DailyAvailability(hotelId, date, 0);

        availability.reserveOne(capacity);

        if (existing != null) {
            existing.setOccupiedRooms(availability.getOccupiedRooms());
            return existing;
        }

        return availabilityMapper.toDailyAvailabilityEntity(availability);
    }
}
