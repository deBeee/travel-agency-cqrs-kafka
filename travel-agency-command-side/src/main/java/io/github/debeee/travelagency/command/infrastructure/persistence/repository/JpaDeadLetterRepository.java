package io.github.debeee.travelagency.command.infrastructure.persistence.repository;

import io.github.debeee.travelagency.command.infrastructure.persistence.entity.DeadLetterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaDeadLetterRepository extends JpaRepository<DeadLetterEntity, UUID> {
}