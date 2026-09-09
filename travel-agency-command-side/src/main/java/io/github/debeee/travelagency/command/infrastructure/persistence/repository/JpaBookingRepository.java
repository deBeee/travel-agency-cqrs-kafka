package io.github.debeee.travelagency.command.infrastructure.persistence.repository;

import io.github.debeee.travelagency.command.infrastructure.persistence.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaBookingRepository extends JpaRepository<BookingEntity, Long> {
}
