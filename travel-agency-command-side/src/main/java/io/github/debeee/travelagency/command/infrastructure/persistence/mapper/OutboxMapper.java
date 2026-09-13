package io.github.debeee.travelagency.command.infrastructure.persistence.mapper;

import io.github.debeee.travelagency.command.application.event.OutboxPayload;
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

    public OutboxEntity toOutboxEntity(OutboxPayload event, String topicName, String aggregateId, String eventType) {
        try {
            String payloadJson = objectMapper.writeValueAsString(event);

            return OutboxEntity.builder()
                    .aggregateId(aggregateId)
                    .type(eventType)
                    .payload(payloadJson)
                    .topic(topicName)
                    .createdAt(LocalDateTime.now())
                    .build();
        } catch (JacksonException e) {
            throw new IllegalStateException("Error serializing %s payload to JSON for outbox".formatted(eventType), e);
        }
    }
}
