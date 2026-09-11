package io.github.debeee.travelagency.command.infrastructure.persistence.repository;

import io.github.debeee.travelagency.command.infrastructure.persistence.entity.OutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaOutboxRepository extends JpaRepository<OutboxEntity, UUID> {
    List<OutboxEntity> findAllByOrderByCreatedAtAsc(Pageable pageable);
}
