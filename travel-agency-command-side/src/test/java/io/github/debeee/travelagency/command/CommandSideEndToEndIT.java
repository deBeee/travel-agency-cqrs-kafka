package io.github.debeee.travelagency.command;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.github.debeee.travelagency.avro.BookingCreatedAvro;
import io.github.debeee.travelagency.avro.HotelUpsertedAvro;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaOutboxRepository;
import io.github.debeee.travelagency.command.presentation.dto.booking.CreateBookingRequestDto;
import io.github.debeee.travelagency.command.presentation.dto.booking.CreateBookingResponseDto;
import io.github.debeee.travelagency.command.presentation.dto.error.ErrorResponseDto;
import io.github.debeee.travelagency.command.presentation.dto.hotel.CreateHotelRequestDto;
import io.github.debeee.travelagency.command.presentation.dto.hotel.CreateHotelResponseDto;
import io.github.debeee.travelagency.command.presentation.dto.hotel.UpdateHotelCapacityRequestDto;
import io.github.debeee.travelagency.command.presentation.dto.hotel.UpdateHotelCapacityResponseDto;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CommandSideEndToEndIT {

    private static final String BOOKINGS_TOPIC = "travel.bookings";
    private static final String HOTELS_TOPIC = "travel.hotels";
    private static final Duration EVENT_TIMEOUT = Duration.ofSeconds(10);
    private static final LocalDate START = LocalDate.now().plusDays(30);
    private static final LocalDate END = LocalDate.now().plusDays(32);

    @Autowired
    private RestTestClient client;

    @Autowired
    private JpaOutboxRepository jpaOutboxRepository;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Test
    void shouldPublishHotelUpsertedEventAndClearOutboxWhenHotelIsCreated() {
        // given
        CreateHotelRequestDto request = new CreateHotelRequestDto(2);
        String expectedEventType = "HotelUpserted";
        int expectedEventCount = 1;

        // when
        EntityExchangeResult<CreateHotelResponseDto> response = client.post().uri("/api/hotels")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .returnResult(CreateHotelResponseDto.class);

        // then
        Long hotelId = response.getResponseBody().hotelId();
        List<ConsumerRecord<String, SpecificRecord>> events = readEventsForHotel(HOTELS_TOPIC, hotelId, expectedEventCount);
        HotelUpsertedAvro event = (HotelUpsertedAvro) events.getFirst().value();
        await().atMost(EVENT_TIMEOUT).untilAsserted(() -> assertThat(jpaOutboxRepository.count()).isZero());
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(hotelId).isNotNull(),
                () -> assertThat(events).hasSize(expectedEventCount),
                () -> assertThat(events.getFirst().key()).isEqualTo(hotelId.toString()),
                () -> assertThat(event.getHotelId()).isEqualTo(hotelId),
                () -> assertThat(event.getCapacity()).isEqualTo(request.capacity()),
                () -> assertThat(eventType(events.getFirst())).isEqualTo(expectedEventType)
        );
    }

    @Test
    void shouldPublishHotelUpsertedEventWithNewCapacityWhenCapacityIsUpdated() {
        // given
        Long hotelId = createHotel(2);
        UpdateHotelCapacityRequestDto request = new UpdateHotelCapacityRequestDto(10);
        UpdateHotelCapacityResponseDto expectedResponse = new UpdateHotelCapacityResponseDto(hotelId, 10);
        List<HotelUpsertedAvro> expectedEvents = List.of(
                HotelUpsertedAvro.newBuilder().setHotelId(hotelId).setCapacity(2L).build(),
                HotelUpsertedAvro.newBuilder().setHotelId(hotelId).setCapacity(10L).build());

        // when
        EntityExchangeResult<UpdateHotelCapacityResponseDto> response = client.put().uri("/api/hotels/{hotelId}", hotelId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .returnResult(UpdateHotelCapacityResponseDto.class);

        // then
        List<ConsumerRecord<String, SpecificRecord>> events = readEventsForHotel(HOTELS_TOPIC, hotelId, expectedEvents.size());
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getResponseBody()).isEqualTo(expectedResponse),
                () -> assertThat(events).extracting(ConsumerRecord::value).containsExactlyElementsOf(expectedEvents)
        );
    }

    @Test
    void shouldPublishBookingCreatedEventKeyedByHotelWhenBookingIsCreated() {
        // given
        Long hotelId = createHotel(2);
        CreateBookingRequestDto request = new CreateBookingRequestDto(hotelId, 100L, START, END);
        String expectedEventType = "BookingCreated";
        int expectedEventCount = 1;

        // when
        EntityExchangeResult<CreateBookingResponseDto> response = client.post().uri("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .returnResult(CreateBookingResponseDto.class);

        // then
        Long bookingId = response.getResponseBody().bookingId();
        List<ConsumerRecord<String, SpecificRecord>> events = readEventsForHotel(BOOKINGS_TOPIC, hotelId, expectedEventCount);
        BookingCreatedAvro event = (BookingCreatedAvro) events.getFirst().value();
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(bookingId).isNotNull(),
                () -> assertThat(events).hasSize(expectedEventCount),
                () -> assertThat(events.getFirst().key()).isEqualTo(hotelId.toString()),
                () -> assertThat(event.getId()).isEqualTo(bookingId),
                () -> assertThat(event.getHotelId()).isEqualTo(hotelId),
                () -> assertThat(event.getUserId()).isEqualTo(request.userId()),
                () -> assertThat(event.getStart()).isEqualTo(request.start().toString()),
                () -> assertThat(event.getEnd()).isEqualTo(request.end().toString()),
                () -> assertThat(eventType(events.getFirst())).isEqualTo(expectedEventType)
        );
    }

    @Test
    void shouldRejectBookingWithConflictAndPublishNoEventWhenHotelIsSoldOut() {
        // given
        Long hotelId = createHotel(2);
        createBooking(hotelId, 100L);
        createBooking(hotelId, 101L);
        CreateBookingRequestDto thirdRequest = new CreateBookingRequestDto(hotelId, 102L, START, END);
        String expectedMessage = "Hotel %d overbooked on %s. Capacity: 2, occupied: 2".formatted(hotelId, START);
        int expectedEventCount = 2;

        // when
        EntityExchangeResult<ErrorResponseDto> response = client.post().uri("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(thirdRequest)
                .exchange()
                .returnResult(ErrorResponseDto.class);

        // then
        List<ConsumerRecord<String, SpecificRecord>> events = readEventsForHotel(BOOKINGS_TOPIC, hotelId, expectedEventCount);
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(HttpStatus.CONFLICT),
                () -> assertThat(response.getResponseBody().message()).isEqualTo(expectedMessage),
                () -> assertThat(events).hasSize(expectedEventCount)
        );
    }

    @Test
    void shouldReturnBadRequestWhenBookingDatesAreInThePast() {
        // given
        Long hotelId = createHotel(2);
        CreateBookingRequestDto request =
                new CreateBookingRequestDto(hotelId, 100L, LocalDate.now().minusDays(2), LocalDate.now().minusDays(1));
        String expectedStartError = "Start date must not be in the past";
        String expectedEndError = "End date must not be in the past";

        // when
        EntityExchangeResult<ErrorResponseDto> response = client.post().uri("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .returnResult(ErrorResponseDto.class);

        // then
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(response.getResponseBody().validationErrors())
                        .containsEntry("start", expectedStartError)
                        .containsEntry("end", expectedEndError)
        );
    }

    @Test
    void shouldAcceptExactlyCapacityBookingsWhenManyClientsBookConcurrently() throws Exception {
        // given
        int capacity = 3;
        int clients = 10;
        Long hotelId = createHotel(capacity);
        CountDownLatch startSignal = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(clients);
        int expectedAcceptedCount = 3;
        int expectedRejectedCount = 7;

        // when
        List<Future<EntityExchangeResult<CreateBookingResponseDto>>> responses = IntStream.range(0, clients)
                .mapToObj(userId -> executor.submit(() -> {
                    startSignal.await();
                    return client.post().uri("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(new CreateBookingRequestDto(hotelId, (long) userId, START, END))
                            .exchange()
                            .returnResult(CreateBookingResponseDto.class);
                }))
                .toList();
        startSignal.countDown();
        List<HttpStatus> statuses = new ArrayList<>();
        List<Long> acceptedBookingIds = new ArrayList<>();
        for (Future<EntityExchangeResult<CreateBookingResponseDto>> future : responses) {
            EntityExchangeResult<CreateBookingResponseDto> response = future.get();
            statuses.add(HttpStatus.valueOf(response.getStatus().value()));
            if (response.getStatus() == HttpStatus.CREATED) {
                acceptedBookingIds.add(response.getResponseBody().bookingId());
            }
        }
        executor.shutdown();

        // then
        List<ConsumerRecord<String, SpecificRecord>> events = readEventsForHotel(BOOKINGS_TOPIC, hotelId, expectedAcceptedCount);
        assertAll(
                () -> assertThat(statuses).filteredOn(status -> status == HttpStatus.CREATED).hasSize(expectedAcceptedCount),
                () -> assertThat(statuses).filteredOn(status -> status == HttpStatus.CONFLICT).hasSize(expectedRejectedCount),
                () -> assertThat(events)
                        .extracting(event -> ((BookingCreatedAvro) event.value()).getId())
                        .containsExactlyInAnyOrderElementsOf(acceptedBookingIds)
        );
    }

    private Long createHotel(long capacity) {
        EntityExchangeResult<CreateHotelResponseDto> response = client.post().uri("/api/hotels")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateHotelRequestDto(capacity))
                .exchange()
                .returnResult(CreateHotelResponseDto.class);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        return response.getResponseBody().hotelId();
    }

    private void createBooking(Long hotelId, Long userId) {
        EntityExchangeResult<CreateBookingResponseDto> response = client.post().uri("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateBookingRequestDto(hotelId, userId, START, END))
                .exchange()
                .returnResult(CreateBookingResponseDto.class);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
    }

    private List<ConsumerRecord<String, SpecificRecord>> readEventsForHotel(String topic, Long hotelId, int expectedCount) {
        try (KafkaConsumer<String, SpecificRecord> consumer = new KafkaConsumer<>(consumerProperties())) {
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

            List<ConsumerRecord<String, SpecificRecord>> matching = new ArrayList<>();
            long deadline = System.currentTimeMillis() + EVENT_TIMEOUT.toMillis();

            while (matching.size() < expectedCount && System.currentTimeMillis() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(record -> {
                    if (hotelId.toString().equals(record.key())) {
                        matching.add(record);
                    }
                });
            }

            consumer.poll(Duration.ofSeconds(2)).forEach(record -> {
                if (hotelId.toString().equals(record.key())) {
                    matching.add(record);
                }
            });
            return matching;
        }
    }

    private Map<String, Object> consumerProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "e2e-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        return props;
    }

    private static String eventType(ConsumerRecord<String, SpecificRecord> record) {
        return new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8);
    }
}
