package io.github.debeee.travelagency.query.infrastructure.streams;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.github.debeee.travelagency.avro.AvailabilityUpdatedAvro;
import io.github.debeee.travelagency.avro.BookingCreatedAvro;
import io.github.debeee.travelagency.query.infrastructure.configuration.properties.AppTopicsProperties;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class BookingStreamsTopologyTest {

    private static final String SCHEMA_REGISTRY_URL = "mock://topology-test";
    private static final String BOOKINGS_TOPIC = "travel.bookings";
    private static final String AVAILABILITY_TOPIC = "travel.availability";
    private static final String OCCUPANCY_STORE = "occupancy-store";

    private TopologyTestDriver driver;
    private TestInputTopic<String, BookingCreatedAvro> bookings;
    private TestOutputTopic<String, AvailabilityUpdatedAvro> availability;

    @BeforeEach
    void setUp() {
        AppTopicsProperties topics = new AppTopicsProperties(
                BOOKINGS_TOPIC, AVAILABILITY_TOPIC, "travel.availability.DLT", "travel.hotels", "travel.hotels.DLT");
        BookingStreamsTopology topology = new BookingStreamsTopology(topics);
        ReflectionTestUtils.setField(topology, "schemaRegistryUrl", SCHEMA_REGISTRY_URL);

        StreamsBuilder builder = new StreamsBuilder();
        topology.dailyAvailabilityTopology(builder);

        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "topology-test");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        config.put(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0);
        config.put(StreamsConfig.STATE_DIR_CONFIG, "target/kafka-streams-topology-test");
        driver = new TopologyTestDriver(builder.build(), config);

        Map<String, Object> serdeConfig = Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);
        SpecificAvroSerde<BookingCreatedAvro> bookingSerde = new SpecificAvroSerde<>();
        bookingSerde.configure(serdeConfig, false);
        SpecificAvroSerde<AvailabilityUpdatedAvro> availabilitySerde = new SpecificAvroSerde<>();
        availabilitySerde.configure(serdeConfig, false);

        bookings = driver.createInputTopic(BOOKINGS_TOPIC, new StringSerializer(), bookingSerde.serializer());
        availability = driver.createOutputTopic(AVAILABILITY_TOPIC, new StringDeserializer(), availabilitySerde.deserializer());
    }

    @AfterEach
    void tearDown() {
        driver.close();
    }

    @Test
    void shouldEmitOneAvailabilityPerNightKeyedByHotelWhenBookingCoversDateRange() {
        // given
        BookingCreatedAvro booking = booking(1L, 7L, "2027-06-01", "2027-06-03");
        List<KeyValue<String, AvailabilityUpdatedAvro>> expectedRecords = List.of(
                KeyValue.pair("7", availability(7L, "2027-06-01", 1L)),
                KeyValue.pair("7", availability(7L, "2027-06-02", 1L)),
                KeyValue.pair("7", availability(7L, "2027-06-03", 1L)));

        // when
        bookings.pipeInput("7", booking);

        // then
        assertThat(availability.readKeyValuesToList()).containsExactlyElementsOf(expectedRecords);
    }

    @Test
    void shouldEmitSingleAvailabilityWhenBookingIsOneNight() {
        // given
        BookingCreatedAvro booking = booking(1L, 7L, "2027-06-01", "2027-06-01");
        List<KeyValue<String, AvailabilityUpdatedAvro>> expectedRecords = List.of(
                KeyValue.pair("7", availability(7L, "2027-06-01", 1L)));

        // when
        bookings.pipeInput("7", booking);

        // then
        assertThat(availability.readKeyValuesToList()).containsExactlyElementsOf(expectedRecords);
    }

    @Test
    void shouldAccumulateOccupancyPerNightWhenBookingsOverlap() {
        // given
        BookingCreatedAvro firstBooking = booking(1L, 7L, "2027-06-01", "2027-06-02");
        BookingCreatedAvro secondBooking = booking(2L, 7L, "2027-06-02", "2027-06-03");
        List<KeyValue<String, AvailabilityUpdatedAvro>> expectedRecords = List.of(
                KeyValue.pair("7", availability(7L, "2027-06-01", 1L)),
                KeyValue.pair("7", availability(7L, "2027-06-02", 1L)),
                KeyValue.pair("7", availability(7L, "2027-06-02", 2L)),
                KeyValue.pair("7", availability(7L, "2027-06-03", 1L)));

        // when
        bookings.pipeInput("7", firstBooking);
        bookings.pipeInput("7", secondBooking);

        // then
        assertThat(availability.readKeyValuesToList()).containsExactlyElementsOf(expectedRecords);
    }

    @Test
    void shouldKeepOccupancyPerHotelAndNightInStateStoreWhenBookingsAreProcessed() {
        // given
        BookingCreatedAvro firstBooking = booking(1L, 7L, "2027-06-01", "2027-06-02");
        BookingCreatedAvro secondBooking = booking(2L, 7L, "2027-06-02", "2027-06-03");
        Map<String, Long> expectedOccupancy = Map.of(
                "7:2027-06-01", 1L,
                "7:2027-06-02", 2L,
                "7:2027-06-03", 1L);

        // when
        bookings.pipeInput("7", firstBooking);
        bookings.pipeInput("7", secondBooking);

        // then
        assertThat(occupancyStoreContents()).isEqualTo(expectedOccupancy);
    }

    @Test
    void shouldCountHotelsIndependentlyWhenBookingsTargetDifferentHotels() {
        // given
        BookingCreatedAvro hotel7Booking = booking(1L, 7L, "2027-06-01", "2027-06-01");
        BookingCreatedAvro hotel8Booking = booking(2L, 8L, "2027-06-01", "2027-06-01");
        List<KeyValue<String, AvailabilityUpdatedAvro>> expectedRecords = List.of(
                KeyValue.pair("7", availability(7L, "2027-06-01", 1L)),
                KeyValue.pair("8", availability(8L, "2027-06-01", 1L)));
        Map<String, Long> expectedOccupancy = Map.of(
                "7:2027-06-01", 1L,
                "8:2027-06-01", 1L);

        // when
        bookings.pipeInput("7", hotel7Booking);
        bookings.pipeInput("8", hotel8Booking);

        // then
        assertThat(availability.readKeyValuesToList()).containsExactlyElementsOf(expectedRecords);
        assertThat(occupancyStoreContents()).isEqualTo(expectedOccupancy);
    }

    private Map<String, Long> occupancyStoreContents() {
        KeyValueStore<String, Long> store = driver.getKeyValueStore(OCCUPANCY_STORE);
        Map<String, Long> contents = new HashMap<>();
        try (KeyValueIterator<String, Long> iterator = store.all()) {
            iterator.forEachRemaining(entry -> contents.put(entry.key, entry.value));
        }
        return contents;
    }

    private static BookingCreatedAvro booking(long id, long hotelId, String start, String end) {
        return BookingCreatedAvro.newBuilder()
                .setId(id)
                .setHotelId(hotelId)
                .setUserId(100L)
                .setStart(start)
                .setEnd(end)
                .build();
    }

    private static AvailabilityUpdatedAvro availability(long hotelId, String date, long occupied) {
        return AvailabilityUpdatedAvro.newBuilder()
                .setHotelId(hotelId)
                .setDate(date)
                .setOccupied(occupied)
                .build();
    }
}
