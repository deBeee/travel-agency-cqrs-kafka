package io.github.debeee.travelagency.query;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.github.debeee.travelagency.avro.AvailabilityUpdatedAvro;
import io.github.debeee.travelagency.avro.BookingCreatedAvro;
import io.github.debeee.travelagency.avro.HotelUpsertedAvro;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityReadRepository;
import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import io.github.debeee.travelagency.query.presentation.dto.AvailabilityResponseDto;
import io.github.debeee.travelagency.query.presentation.dto.AvailabilityStatusDto;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class QuerySideEndToEndIT {

    private static final String BOOKINGS_TOPIC = "travel.bookings";
    private static final String HOTELS_TOPIC = "travel.hotels";
    private static final String AVAILABILITY_TOPIC = "travel.availability";
    private static final String AVAILABILITY_DLT = "travel.availability.DLT";
    private static final String HOTELS_DLT = "travel.hotels.DLT";
    private static final Duration PROJECTION_TIMEOUT = Duration.ofSeconds(30);
    private static final LocalDate JUNE_1 = LocalDate.of(2027, 6, 1);
    private static final LocalDate JUNE_2 = LocalDate.of(2027, 6, 2);
    private static final LocalDate JUNE_3 = LocalDate.of(2027, 6, 3);
    private static final AtomicLong HOTEL_IDS = new AtomicLong(1_000);

    @Autowired
    private AvailabilityReadRepository availabilityReadRepository;

    @Autowired
    private RestTestClient client;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    private KafkaProducer<String, SpecificRecord> avroProducer;
    private KafkaProducer<String, byte[]> rawProducer;

    @BeforeEach
    void setUp() {
        avroProducer = new KafkaProducer<>(producerProperties(KafkaAvroSerializer.class));
        rawProducer = new KafkaProducer<>(producerProperties(ByteArraySerializer.class));
    }

    @AfterEach
    void tearDown() {
        avroProducer.close();
        rawProducer.close();
    }

    @Test
    void shouldProjectOneDayPerNightWithHotelCapacityWhenBookingIsCreated() throws Exception {
        // given
        long hotelId = HOTEL_IDS.incrementAndGet();
        publishHotel(hotelId, 2);
        BookingCreatedAvro booking = booking(1L, hotelId, JUNE_1, JUNE_3);
        List<Availability> expectedAvailabilities = List.of(
                new Availability(hotelId, JUNE_1, 1, 2, AvailabilityStatus.LAST_ROOMS),
                new Availability(hotelId, JUNE_2, 1, 2, AvailabilityStatus.LAST_ROOMS),
                new Availability(hotelId, JUNE_3, 1, 2, AvailabilityStatus.LAST_ROOMS));
        List<AvailabilityResponseDto> expectedResponse = List.of(
                new AvailabilityResponseDto(hotelId, JUNE_1, 1, 2, 1, AvailabilityStatusDto.LAST_ROOMS),
                new AvailabilityResponseDto(hotelId, JUNE_2, 1, 2, 1, AvailabilityStatusDto.LAST_ROOMS),
                new AvailabilityResponseDto(hotelId, JUNE_3, 1, 2, 1, AvailabilityStatusDto.LAST_ROOMS));

        // when
        avroProducer.send(new ProducerRecord<>(BOOKINGS_TOPIC, String.valueOf(hotelId), booking)).get();

        // then
        await().atMost(PROJECTION_TIMEOUT).untilAsserted(() ->
                assertThat(availabilityReadRepository.findByHotel(hotelId, null, null)).isEqualTo(expectedAvailabilities));
        EntityExchangeResult<List<AvailabilityResponseDto>> response = client.get()
                .uri("/api/availability/{hotelId}", hotelId)
                .exchange()
                .returnResult(new ParameterizedTypeReference<>() {
                });
        assertAll(
                () -> assertThat(response.getStatus()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getResponseBody()).isEqualTo(expectedResponse)
        );
    }

    @Test
    void shouldMarkNightSoldOutWhenBookingsFillCapacity() throws Exception {
        // given
        long hotelId = HOTEL_IDS.incrementAndGet();
        publishHotel(hotelId, 2);
        BookingCreatedAvro firstBooking = booking(1L, hotelId, JUNE_1, JUNE_1);
        BookingCreatedAvro secondBooking = booking(2L, hotelId, JUNE_1, JUNE_1);
        List<Availability> expectedAvailabilities = List.of(
                new Availability(hotelId, JUNE_1, 2, 2, AvailabilityStatus.SOLD_OUT));

        // when
        avroProducer.send(new ProducerRecord<>(BOOKINGS_TOPIC, String.valueOf(hotelId), firstBooking)).get();
        avroProducer.send(new ProducerRecord<>(BOOKINGS_TOPIC, String.valueOf(hotelId), secondBooking)).get();

        // then
        await().atMost(PROJECTION_TIMEOUT).untilAsserted(() ->
                assertThat(availabilityReadRepository.findByHotel(hotelId, null, null)).isEqualTo(expectedAvailabilities));
    }

    @Test
    void shouldReprojectAlreadyProjectedDaysWhenHotelCapacityGrows() throws Exception {
        // given
        long hotelId = HOTEL_IDS.incrementAndGet();
        publishHotel(hotelId, 2);
        avroProducer.send(new ProducerRecord<>(BOOKINGS_TOPIC, String.valueOf(hotelId), booking(1L, hotelId, JUNE_1, JUNE_2))).get();
        avroProducer.send(new ProducerRecord<>(BOOKINGS_TOPIC, String.valueOf(hotelId), booking(2L, hotelId, JUNE_1, JUNE_2))).get();
        await().atMost(PROJECTION_TIMEOUT).untilAsserted(() ->
                assertThat(availabilityReadRepository.findByHotel(hotelId, null, null))
                        .extracting(Availability::status)
                        .containsExactly(AvailabilityStatus.SOLD_OUT, AvailabilityStatus.SOLD_OUT));
        List<Availability> expectedAvailabilities = List.of(
                new Availability(hotelId, JUNE_1, 2, 10, AvailabilityStatus.AVAILABLE),
                new Availability(hotelId, JUNE_2, 2, 10, AvailabilityStatus.AVAILABLE));

        // when
        publishHotel(hotelId, 10);

        // then
        await().atMost(PROJECTION_TIMEOUT).untilAsserted(() ->
                assertThat(availabilityReadRepository.findByHotel(hotelId, null, null)).isEqualTo(expectedAvailabilities));
    }

    @Test
    void shouldFallBackToDefaultCapacityWhenBookingArrivesBeforeHotel() throws Exception {
        // given
        long hotelId = HOTEL_IDS.incrementAndGet();
        BookingCreatedAvro booking = booking(1L, hotelId, JUNE_1, JUNE_1);
        List<Availability> expectedAvailabilities = List.of(
                new Availability(hotelId, JUNE_1, 1, 100, AvailabilityStatus.AVAILABLE));

        // when
        avroProducer.send(new ProducerRecord<>(BOOKINGS_TOPIC, String.valueOf(hotelId), booking)).get();

        // then
        await().atMost(PROJECTION_TIMEOUT).untilAsserted(() ->
                assertThat(availabilityReadRepository.findByHotel(hotelId, null, null)).isEqualTo(expectedAvailabilities));
    }

    @Test
    void shouldKeepReadModelUnchangedWhenSameAvailabilityEventIsDeliveredTwice() throws Exception {
        // given
        long hotelId = HOTEL_IDS.incrementAndGet();
        publishHotel(hotelId, 10);
        AvailabilityUpdatedAvro event = availabilityUpdated(hotelId, JUNE_1, 3);
        List<Availability> expectedAvailabilities = List.of(
                new Availability(hotelId, JUNE_1, 3, 10, AvailabilityStatus.AVAILABLE));

        // when
        avroProducer.send(new ProducerRecord<>(AVAILABILITY_TOPIC, String.valueOf(hotelId), event)).get();
        avroProducer.send(new ProducerRecord<>(AVAILABILITY_TOPIC, String.valueOf(hotelId), event)).get();

        // then
        await().atMost(PROJECTION_TIMEOUT).untilAsserted(() ->
                assertThat(availabilityReadRepository.findByHotel(hotelId, null, null)).isEqualTo(expectedAvailabilities));
    }

    @Test
    void shouldRouteUndeserializableAvailabilityRecordToDltWithoutBlockingPartition() throws Exception {
        // given
        long hotelId = HOTEL_IDS.incrementAndGet();
        publishHotel(hotelId, 10);
        String key = String.valueOf(hotelId);
        byte[] poison = "this is not avro".getBytes(StandardCharsets.UTF_8);
        AvailabilityUpdatedAvro validEvent = availabilityUpdated(hotelId, JUNE_1, 3);
        List<Availability> expectedAvailabilities = List.of(
                new Availability(hotelId, JUNE_1, 3, 10, AvailabilityStatus.AVAILABLE));
        int expectedDltRecordCount = 1;

        // when
        rawProducer.send(new ProducerRecord<>(AVAILABILITY_TOPIC, key, poison)).get();
        avroProducer.send(new ProducerRecord<>(AVAILABILITY_TOPIC, key, validEvent)).get();

        // then
        await().atMost(PROJECTION_TIMEOUT).untilAsserted(() ->
                assertThat(availabilityReadRepository.findByHotel(hotelId, null, null)).isEqualTo(expectedAvailabilities));
        List<ConsumerRecord<String, byte[]>> dltRecords = readDltRecordsForKey(AVAILABILITY_DLT, key, expectedDltRecordCount);
        assertAll(
                () -> assertThat(dltRecords).hasSize(expectedDltRecordCount),
                () -> assertThat(dltRecords.getFirst().value()).isEqualTo(poison)
        );
    }

    @Test
    void shouldRouteUndeserializableHotelRecordToHotelsDlt() throws Exception {
        // given
        long hotelId = HOTEL_IDS.incrementAndGet();
        String key = String.valueOf(hotelId);
        byte[] poison = "this is not avro either".getBytes(StandardCharsets.UTF_8);
        int expectedDltRecordCount = 1;

        // when
        rawProducer.send(new ProducerRecord<>(HOTELS_TOPIC, key, poison)).get();

        // then
        List<ConsumerRecord<String, byte[]>> dltRecords = readDltRecordsForKey(HOTELS_DLT, key, expectedDltRecordCount);
        assertAll(
                () -> assertThat(dltRecords).hasSize(expectedDltRecordCount),
                () -> assertThat(dltRecords.getFirst().value()).isEqualTo(poison)
        );
    }

    private void publishHotel(long hotelId, long capacity) throws Exception {
        HotelUpsertedAvro hotel = HotelUpsertedAvro.newBuilder().setHotelId(hotelId).setCapacity(capacity).build();
        avroProducer.send(new ProducerRecord<>(HOTELS_TOPIC, String.valueOf(hotelId), hotel)).get();
    }

    private static BookingCreatedAvro booking(long id, long hotelId, LocalDate start, LocalDate end) {
        return BookingCreatedAvro.newBuilder()
                .setId(id)
                .setHotelId(hotelId)
                .setUserId(100L)
                .setStart(start.toString())
                .setEnd(end.toString())
                .build();
    }

    private static AvailabilityUpdatedAvro availabilityUpdated(long hotelId, LocalDate date, long occupied) {
        return AvailabilityUpdatedAvro.newBuilder()
                .setHotelId(hotelId)
                .setDate(date.toString())
                .setOccupied(occupied)
                .build();
    }

    private List<ConsumerRecord<String, byte[]>> readDltRecordsForKey(String topic, String key, int expectedCount) {
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(dltConsumerProperties())) {
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

            List<ConsumerRecord<String, byte[]>> matching = new ArrayList<>();
            long deadline = System.currentTimeMillis() + PROJECTION_TIMEOUT.toMillis();
            while (matching.size() < expectedCount && System.currentTimeMillis() < deadline) {
                consumer.poll(Duration.ofMillis(500)).forEach(record -> {
                    if (key.equals(record.key())) {
                        matching.add(record);
                    }
                });
            }
            consumer.poll(Duration.ofSeconds(2)).forEach(record -> {
                if (key.equals(record.key())) {
                    matching.add(record);
                }
            });
            return matching;
        }
    }

    private Map<String, Object> producerProperties(Class<?> valueSerializer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        return props;
    }

    private Map<String, Object> dltConsumerProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "e2e-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        return props;
    }
}
