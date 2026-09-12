package io.github.debeee.travelagency.command.infrastructure.kafka.outbox;

import io.github.debeee.travelagency.avro.BookingCreatedAvro;
import io.github.debeee.travelagency.command.domain.model.Booking;
import io.github.debeee.travelagency.command.infrastructure.kafka.properties.BookingTopicProperties;
import io.github.debeee.travelagency.command.infrastructure.kafka.properties.OutboxProperties;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.DeadLetterEntity;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.OutboxEntity;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaDeadLetterRepository;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final JpaOutboxRepository jpaOutboxRepository;
    private final JpaDeadLetterRepository jpaDeadLetterRepository;
    private final OutboxProperties outboxProperties;
    private final BookingTopicProperties bookingTopicProperties;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

    @Scheduled(fixedDelayString =  "${kafka.outbox.poll-interval}")
    public void processOutbox() {
        List<OutboxEntity> entries = jpaOutboxRepository.findAllByOrderByCreatedAtAsc(
                PageRequest.of(0, outboxProperties.batchSize())
        );  

        for (OutboxEntity entry : entries) {
            try {
                sendToKafka(entry, toAvro(entry));
                jpaOutboxRepository.delete(entry);
            } catch (PayloadConversionException e) {
                moveToDeadLetter(entry, e);
                jpaOutboxRepository.delete(entry);
                log.error("Outbox entry {} moved to dead letter: {}", entry.getId(), e.getMessage());
            } catch (Exception e) {
                handleTransportFailure(entry, e);
                break;
            }
        }
    }

    private void sendToKafka(OutboxEntity entry, SpecificRecordBase avro) {
        String topic = entry.getTopic() != null
                ? entry.getTopic()
                : bookingTopicProperties.name();

        var record = new ProducerRecord<>(topic, entry.getAggregateId(), avro);

        record.headers().add(
                "eventType",
                entry.getType().getBytes(StandardCharsets.UTF_8)
        );

        try {
            kafkaTemplate.send(record).get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new OutboxSendException("Interrupted while sending outbox entry " + entry.getId(), ie);
        } catch (ExecutionException ee) {
            throw new OutboxSendException("Kafka send failed for outbox entry " + entry.getId(), ee.getCause());
        }
    }

    private SpecificRecordBase toAvro(OutboxEntity entry) {
        try {
            return switch (entry.getType()) {
                case "BookingCreated" -> {
                    Booking booking = objectMapper.readValue(entry.getPayload(), Booking.class);
                    yield BookingCreatedAvro.newBuilder()
                            .setId(booking.id())
                            .setHotelId(booking.hotelId())
                            .setUserId(booking.userId())
                            .setStart(booking.start().toString())
                            .setEnd(booking.end().toString())
                            .build();
                }
                default -> throw new IllegalArgumentException("Unknown event type: " + entry.getType());
            };
        } catch (Exception e) {
            throw new PayloadConversionException("Cannot convert outbox payload to Avro, type=" + entry.getType(), e);
        }
    }
    
    private void handleTransportFailure(OutboxEntity entry, Exception e) {
        entry.incrementRetryCount();
        jpaOutboxRepository.save(entry);

        if (entry.hasExceededRetryThreshold(outboxProperties.alertAfterRetries())) {
            log.error("Outbox entry {} still failing after {} attempts — check broker/network: {}",
                    entry.getId(), entry.getRetryCount(), e.getMessage());
        } else {
            log.warn("Outbox entry {} failed (transient, attempt {}): {}. Will retry.",
                    entry.getId(), entry.getRetryCount(), e.getMessage());
        }
    }

    private void moveToDeadLetter(OutboxEntity entry, Exception e) {
        var deadLetter = DeadLetterEntity.builder()
                .originalOutboxId(entry.getId())
                .aggregatedId(entry.getAggregateId())
                .type(entry.getType())
                .payload(entry.getPayload())
                .errorMessage(e.getMessage())
                .createdAt(entry.getCreatedAt())
                .failedAt(LocalDateTime.now())
                .retryCount(entry.getRetryCount())
                .build();
        jpaDeadLetterRepository.save(deadLetter);
    }
}
