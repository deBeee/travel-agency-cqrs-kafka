package io.github.debeee.travelagency.command.infrastructure.persistence.adapter.outbox;

import io.github.debeee.travelagency.command.application.event.BookingCreatedPayload;
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

    private static final String BOOKING_CREATED_EVENT_TYPE = "BookingCreated";

    @Override
    public void saveOutbox(Booking booking) {
        var payload = new BookingCreatedPayload(
                booking.id(),
                booking.hotelId(),
                booking.userId(),
                booking.start(),
                booking.end()
        );

        var outboxEntity = outboxMapper.toOutboxEntity(
                payload,
                bookingTopicProperties.name(),
                booking.hotelId().toString(),
                BOOKING_CREATED_EVENT_TYPE
        );

        jpaOutboxRepository.save(outboxEntity);
    }
}
