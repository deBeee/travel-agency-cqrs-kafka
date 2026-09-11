package io.github.debeee.travelagency.command.infrastructure.persistence.adapter.outbox;

import io.github.debeee.travelagency.command.application.port.out.OutboxRepository;
import io.github.debeee.travelagency.command.domain.model.Booking;
import io.github.debeee.travelagency.command.infrastructure.kafka.properties.BookingTopicProperties;
import io.github.debeee.travelagency.command.infrastructure.persistence.mapper.OutboxMapper;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaBookingOutboxRepositoryAdapter implements OutboxRepository<Booking> {

    private final BookingTopicProperties bookingTopicProperties;
    private final JpaOutboxRepository jpaOutboxRepository;
    private final OutboxMapper outboxMapper;

    @Override
    public void saveOutbox(Booking booking) {
        var outboxEntity = outboxMapper.toBookingsOutboxEntity(booking, bookingTopicProperties.name());
        jpaOutboxRepository.save(outboxEntity);
    }
}
