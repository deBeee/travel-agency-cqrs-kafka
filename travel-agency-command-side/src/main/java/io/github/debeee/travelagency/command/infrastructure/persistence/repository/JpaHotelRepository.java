package io.github.debeee.travelagency.command.infrastructure.persistence.repository;

import io.github.debeee.travelagency.command.infrastructure.persistence.entity.HotelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaHotelRepository extends JpaRepository<HotelEntity, Long> {
}
