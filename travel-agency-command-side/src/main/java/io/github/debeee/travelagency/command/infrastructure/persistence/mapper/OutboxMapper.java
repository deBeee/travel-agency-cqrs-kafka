package io.github.debeee.travelagency.command.infrastructure.persistence.mapper;

import io.github.debeee.travelagency.command.domain.model.Booking;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.OutboxEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxMapper {

    private final ObjectMapper objectMapper;

    public OutboxEntity toBookingsOutboxEntity(Booking booking, String bookingTopicName) {
        try {
            String payloadJson = objectMapper.writeValueAsString(booking);

            return OutboxEntity.builder()
                    .aggregateId(booking.hotelId().toString())
                    .type("BookingCreated")
                    .payload(payloadJson)
                    .topic(bookingTopicName)
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (JacksonException e) {
            throw new IllegalStateException("Error serializing Booking to JSON for Outbox", e);
        }
    }
}
