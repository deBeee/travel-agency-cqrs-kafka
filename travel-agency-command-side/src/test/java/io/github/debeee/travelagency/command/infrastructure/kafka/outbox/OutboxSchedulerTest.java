package io.github.debeee.travelagency.command.infrastructure.kafka.outbox;

import io.github.debeee.travelagency.avro.BookingCreatedAvro;
import io.github.debeee.travelagency.avro.HotelUpsertedAvro;
import io.github.debeee.travelagency.command.infrastructure.kafka.properties.BookingTopicProperties;
import io.github.debeee.travelagency.command.infrastructure.kafka.properties.OutboxProperties;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.DeadLetterEntity;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.OutboxEntity;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaDeadLetterRepository;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaOutboxRepository;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TimeoutException;
import org.instancio.Instancio;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@ExtendWith({MockitoExtension.class, InstancioExtension.class, OutputCaptureExtension.class})
class OutboxSchedulerTest {

    private static final String BOOKINGS_TOPIC = "travel.bookings";
    private static final String HOTELS_TOPIC = "travel.hotels";
    private static final int BATCH_SIZE = 50;
    private static final int ALERT_AFTER_RETRIES = 5;
    private static final String BOOKING_CREATED_JSON =
            "{\"id\":42,\"hotelId\":7,\"userId\":100,\"start\":\"2027-06-01\",\"end\":\"2027-06-03\"}";
    private static final String HOTEL_UPSERTED_JSON = "{\"hotelId\":7,\"capacity\":10}";

    @Mock
    private JpaOutboxRepository jpaOutboxRepository;

    @Mock
    private JpaDeadLetterRepository jpaDeadLetterRepository;

    @Mock
    private KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;

    private OutboxScheduler outboxScheduler;

    @BeforeEach
    void setUp() {
        outboxScheduler = new OutboxScheduler(
                jpaOutboxRepository,
                jpaDeadLetterRepository,
                new OutboxProperties(1000, BATCH_SIZE, ALERT_AFTER_RETRIES),
                new BookingTopicProperties(BOOKINGS_TOPIC, 3, 1),
                new ObjectMapper(),
                kafkaTemplate
        );
    }

    @Test
    void shouldFetchOldestEntriesUpToBatchSizeAndDoNothingWhenOutboxIsEmpty() {
        // given
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE)))
                .willReturn(List.of());

        // when
        outboxScheduler.processOutbox();

        // then
        then(jpaOutboxRepository).should().findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE));
        then(jpaOutboxRepository).shouldHaveNoMoreInteractions();
        then(kafkaTemplate).shouldHaveNoInteractions();
        then(jpaDeadLetterRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldPublishBookingCreatedAvroThenDeleteEntryWhenPayloadIsValid() {
        // given
        OutboxEntity entry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "BookingCreated")
                .set(field(OutboxEntity::getTopic), BOOKINGS_TOPIC)
                .set(field(OutboxEntity::getAggregateId), "7")
                .set(field(OutboxEntity::getPayload), BOOKING_CREATED_JSON)
                .create();
        BookingCreatedAvro expectedAvro = BookingCreatedAvro.newBuilder()
                .setId(42L)
                .setHotelId(7L)
                .setUserId(100L)
                .setStart("2027-06-01")
                .setEnd("2027-06-03")
                .build();
        ProducerRecord<String, SpecificRecordBase> expectedRecord = new ProducerRecord<>(BOOKINGS_TOPIC, "7", expectedAvro);
        expectedRecord.headers().add("eventType", "BookingCreated".getBytes(UTF_8));
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE))).willReturn(List.of(entry));
        given(kafkaTemplate.send(expectedRecord)).willReturn(CompletableFuture.completedFuture(null));
        InOrder inOrder = inOrder(kafkaTemplate, jpaOutboxRepository);

        // when
        outboxScheduler.processOutbox();

        // then
        then(kafkaTemplate).should(inOrder).send(expectedRecord);
        then(jpaOutboxRepository).should(inOrder).delete(entry);
        then(jpaDeadLetterRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldPublishHotelUpsertedAvroThenDeleteEntryWhenPayloadIsValid() {
        // given
        OutboxEntity entry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "HotelUpserted")
                .set(field(OutboxEntity::getTopic), HOTELS_TOPIC)
                .set(field(OutboxEntity::getAggregateId), "7")
                .set(field(OutboxEntity::getPayload), HOTEL_UPSERTED_JSON)
                .create();
        HotelUpsertedAvro expectedAvro = HotelUpsertedAvro.newBuilder()
                .setHotelId(7L)
                .setCapacity(10L)
                .build();
        ProducerRecord<String, SpecificRecordBase> expectedRecord = new ProducerRecord<>(HOTELS_TOPIC, "7", expectedAvro);
        expectedRecord.headers().add("eventType", "HotelUpserted".getBytes(UTF_8));
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE))).willReturn(List.of(entry));
        given(kafkaTemplate.send(expectedRecord)).willReturn(CompletableFuture.completedFuture(null));
        InOrder inOrder = inOrder(kafkaTemplate, jpaOutboxRepository);

        // when
        outboxScheduler.processOutbox();

        // then
        then(kafkaTemplate).should(inOrder).send(expectedRecord);
        then(jpaOutboxRepository).should(inOrder).delete(entry);
        then(jpaDeadLetterRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldPublishToBookingsTopicWhenEntryHasNoTopic() {
        // given
        OutboxEntity entry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "BookingCreated")
                .ignore(field(OutboxEntity::getTopic))
                .set(field(OutboxEntity::getAggregateId), "7")
                .set(field(OutboxEntity::getPayload), BOOKING_CREATED_JSON)
                .create();
        BookingCreatedAvro expectedAvro = BookingCreatedAvro.newBuilder()
                .setId(42L)
                .setHotelId(7L)
                .setUserId(100L)
                .setStart("2027-06-01")
                .setEnd("2027-06-03")
                .build();
        ProducerRecord<String, SpecificRecordBase> expectedRecord = new ProducerRecord<>(BOOKINGS_TOPIC, "7", expectedAvro);
        expectedRecord.headers().add("eventType", "BookingCreated".getBytes(UTF_8));
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE))).willReturn(List.of(entry));
        given(kafkaTemplate.send(expectedRecord)).willReturn(CompletableFuture.completedFuture(null));

        // when
        outboxScheduler.processOutbox();

        // then
        then(kafkaTemplate).should().send(expectedRecord);
        then(jpaOutboxRepository).should().delete(entry);
    }

    @Test
    void shouldMoveEntryToDeadLetterAndDeleteItWhenEventTypeIsUnknown() {
        // given
        OutboxEntity poisonEntry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "UnknownEvent")
                .create();
        ArgumentCaptor<DeadLetterEntity> deadLetterCaptor = ArgumentCaptor.forClass(DeadLetterEntity.class);
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE))).willReturn(List.of(poisonEntry));
        String expectedErrorMessage = "Cannot convert outbox payload to Avro, type=UnknownEvent";

        // when
        outboxScheduler.processOutbox();

        // then
        then(jpaDeadLetterRepository).should().save(deadLetterCaptor.capture());
        then(jpaOutboxRepository).should().delete(poisonEntry);
        then(kafkaTemplate).shouldHaveNoInteractions();
        DeadLetterEntity deadLetter = deadLetterCaptor.getValue();
        assertAll(
                () -> assertThat(deadLetter.getOriginalOutboxId()).isEqualTo(poisonEntry.getId()),
                () -> assertThat(deadLetter.getAggregatedId()).isEqualTo(poisonEntry.getAggregateId()),
                () -> assertThat(deadLetter.getType()).isEqualTo(poisonEntry.getType()),
                () -> assertThat(deadLetter.getPayload()).isEqualTo(poisonEntry.getPayload()),
                () -> assertThat(deadLetter.getErrorMessage()).isEqualTo(expectedErrorMessage),
                () -> assertThat(deadLetter.getCreatedAt()).isEqualTo(poisonEntry.getCreatedAt()),
                () -> assertThat(deadLetter.getRetryCount()).isEqualTo(poisonEntry.getRetryCount()),
                () -> assertThat(deadLetter.getFailedAt()).isNotNull()
        );
    }

    @Test
    void shouldMoveEntryToDeadLetterAndDeleteItWhenPayloadIsNotValidJson() {
        // given
        OutboxEntity poisonEntry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "BookingCreated")
                .set(field(OutboxEntity::getPayload), "this is not json")
                .create();
        ArgumentCaptor<DeadLetterEntity> deadLetterCaptor = ArgumentCaptor.forClass(DeadLetterEntity.class);
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE))).willReturn(List.of(poisonEntry));

        // when
        outboxScheduler.processOutbox();

        // then
        then(jpaDeadLetterRepository).should().save(deadLetterCaptor.capture());
        then(jpaOutboxRepository).should().delete(poisonEntry);
        then(kafkaTemplate).shouldHaveNoInteractions();
        assertThat(deadLetterCaptor.getValue().getPayload()).isEqualTo(poisonEntry.getPayload());
    }

    @Test
    void shouldContinueWithNextEntryWhenPreviousEntryIsPoison() {
        // given
        OutboxEntity poisonEntry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "UnknownEvent")
                .create();
        OutboxEntity validEntry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "HotelUpserted")
                .set(field(OutboxEntity::getTopic), HOTELS_TOPIC)
                .set(field(OutboxEntity::getAggregateId), "7")
                .set(field(OutboxEntity::getPayload), HOTEL_UPSERTED_JSON)
                .create();
        HotelUpsertedAvro expectedAvro = HotelUpsertedAvro.newBuilder()
                .setHotelId(7L)
                .setCapacity(10L)
                .build();
        ProducerRecord<String, SpecificRecordBase> expectedRecord = new ProducerRecord<>(HOTELS_TOPIC, "7", expectedAvro);
        expectedRecord.headers().add("eventType", "HotelUpserted".getBytes(UTF_8));
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE)))
                .willReturn(List.of(poisonEntry, validEntry));
        given(kafkaTemplate.send(expectedRecord)).willReturn(CompletableFuture.completedFuture(null));

        // when
        outboxScheduler.processOutbox();

        // then
        then(jpaOutboxRepository).should().delete(poisonEntry);
        then(kafkaTemplate).should().send(expectedRecord);
        then(jpaOutboxRepository).should().delete(validEntry);
    }

    @Test
    void shouldIncrementRetryCountAndKeepEntryWhenKafkaSendFails() {
        // given
        OutboxEntity entry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "HotelUpserted")
                .set(field(OutboxEntity::getTopic), HOTELS_TOPIC)
                .set(field(OutboxEntity::getAggregateId), "7")
                .set(field(OutboxEntity::getPayload), HOTEL_UPSERTED_JSON)
                .set(field(OutboxEntity::getRetryCount), 0)
                .create();
        HotelUpsertedAvro expectedAvro = HotelUpsertedAvro.newBuilder()
                .setHotelId(7L)
                .setCapacity(10L)
                .build();
        ProducerRecord<String, SpecificRecordBase> expectedRecord = new ProducerRecord<>(HOTELS_TOPIC, "7", expectedAvro);
        expectedRecord.headers().add("eventType", "HotelUpserted".getBytes(UTF_8));
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE))).willReturn(List.of(entry));
        given(kafkaTemplate.send(expectedRecord))
                .willReturn(CompletableFuture.failedFuture(new TimeoutException("Broker unavailable")));
        int expectedRetryCount = 1;

        // when
        outboxScheduler.processOutbox();

        // then
        assertThat(entry.getRetryCount()).isEqualTo(expectedRetryCount);
        then(jpaOutboxRepository).should().save(entry);
        then(jpaOutboxRepository).should(never()).delete(entry);
        then(jpaDeadLetterRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldStopBatchWithoutTouchingLaterEntriesWhenKafkaSendFails() {
        // given
        OutboxEntity failingEntry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "HotelUpserted")
                .set(field(OutboxEntity::getTopic), HOTELS_TOPIC)
                .set(field(OutboxEntity::getAggregateId), "7")
                .set(field(OutboxEntity::getPayload), HOTEL_UPSERTED_JSON)
                .create();
        OutboxEntity laterEntry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "BookingCreated")
                .set(field(OutboxEntity::getTopic), BOOKINGS_TOPIC)
                .set(field(OutboxEntity::getAggregateId), "7")
                .set(field(OutboxEntity::getPayload), BOOKING_CREATED_JSON)
                .create();
        HotelUpsertedAvro failingAvro = HotelUpsertedAvro.newBuilder()
                .setHotelId(7L)
                .setCapacity(10L)
                .build();
        ProducerRecord<String, SpecificRecordBase> failingRecord = new ProducerRecord<>(HOTELS_TOPIC, "7", failingAvro);
        failingRecord.headers().add("eventType", "HotelUpserted".getBytes(UTF_8));
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE)))
                .willReturn(List.of(failingEntry, laterEntry));
        given(kafkaTemplate.send(failingRecord))
                .willReturn(CompletableFuture.failedFuture(new TimeoutException("Broker unavailable")));

        // when
        outboxScheduler.processOutbox();

        // then
        then(kafkaTemplate).should().send(failingRecord);
        then(kafkaTemplate).shouldHaveNoMoreInteractions();
        then(jpaOutboxRepository).should().findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE));
        then(jpaOutboxRepository).should().save(failingEntry);
        then(jpaOutboxRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void shouldLogWarningWhenRetryCountIsBelowAlertThreshold(CapturedOutput output) {
        // given
        OutboxEntity entry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "HotelUpserted")
                .set(field(OutboxEntity::getTopic), HOTELS_TOPIC)
                .set(field(OutboxEntity::getAggregateId), "7")
                .set(field(OutboxEntity::getPayload), HOTEL_UPSERTED_JSON)
                .set(field(OutboxEntity::getRetryCount), 0)
                .create();
        HotelUpsertedAvro expectedAvro = HotelUpsertedAvro.newBuilder()
                .setHotelId(7L)
                .setCapacity(10L)
                .build();
        ProducerRecord<String, SpecificRecordBase> expectedRecord = new ProducerRecord<>(HOTELS_TOPIC, "7", expectedAvro);
        expectedRecord.headers().add("eventType", "HotelUpserted".getBytes(UTF_8));
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE))).willReturn(List.of(entry));
        given(kafkaTemplate.send(expectedRecord))
                .willReturn(CompletableFuture.failedFuture(new TimeoutException("Broker unavailable")));
        String expectedWarning = "failed (transient, attempt 1)";
        String unexpectedAlert = "still failing";

        // when
        outboxScheduler.processOutbox();

        // then
        assertAll(
                () -> assertThat(output).contains(expectedWarning),
                () -> assertThat(output).doesNotContain(unexpectedAlert)
        );
    }

    @Test
    void shouldLogErrorWhenRetryCountReachesAlertThreshold(CapturedOutput output) {
        // given
        OutboxEntity entry = Instancio.of(OutboxEntity.class)
                .set(field(OutboxEntity::getType), "HotelUpserted")
                .set(field(OutboxEntity::getTopic), HOTELS_TOPIC)
                .set(field(OutboxEntity::getAggregateId), "7")
                .set(field(OutboxEntity::getPayload), HOTEL_UPSERTED_JSON)
                .set(field(OutboxEntity::getRetryCount), ALERT_AFTER_RETRIES - 1)
                .create();
        HotelUpsertedAvro expectedAvro = HotelUpsertedAvro.newBuilder()
                .setHotelId(7L)
                .setCapacity(10L)
                .build();
        ProducerRecord<String, SpecificRecordBase> expectedRecord = new ProducerRecord<>(HOTELS_TOPIC, "7", expectedAvro);
        expectedRecord.headers().add("eventType", "HotelUpserted".getBytes(UTF_8));
        given(jpaOutboxRepository.findAllByOrderByCreatedAtAsc(PageRequest.of(0, BATCH_SIZE))).willReturn(List.of(entry));
        given(kafkaTemplate.send(expectedRecord))
                .willReturn(CompletableFuture.failedFuture(new TimeoutException("Broker unavailable")));
        String expectedAlert = "still failing after 5 attempts";

        // when
        outboxScheduler.processOutbox();

        // then
        assertThat(output).contains(expectedAlert);
    }
}
