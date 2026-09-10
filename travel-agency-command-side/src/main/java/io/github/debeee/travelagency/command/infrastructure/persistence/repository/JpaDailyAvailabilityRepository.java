package io.github.debeee.travelagency.command.infrastructure.persistence.repository;

import io.github.debeee.travelagency.command.infrastructure.persistence.entity.DailyAvailabilityEntity;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.DailyAvailabilityId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface JpaDailyAvailabilityRepository extends JpaRepository<DailyAvailabilityEntity, DailyAvailabilityId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select dae from DailyAvailabilityEntity dae
            where dae.hotelId = :hotelId
            and dae.date >= :startDate
            and dae.date <= :endDate
            """)
    List<DailyAvailabilityEntity> findAndLockByHotelAndDateRange(Long hotelId, LocalDate startDate, LocalDate endDate);
}
