package io.github.debeee.travelagency.query.infrastructure.kafka;

import io.github.debeee.travelagency.query.infrastructure.configuration.properties.AppTopicsProperties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DltTopicResolverTest {

    private static final String BOOKINGS_TOPIC = "travel.bookings";
    private static final String AVAILABILITY_TOPIC = "travel.availability";
    private static final String AVAILABILITY_DLT = "travel.availability.DLT";
    private static final String HOTELS_TOPIC = "travel.hotels";
    private static final String HOTELS_DLT = "travel.hotels.DLT";

    private DltTopicResolver dltTopicResolver;

    @BeforeEach
    void setUp() {
        dltTopicResolver = new DltTopicResolver(new AppTopicsProperties(
                BOOKINGS_TOPIC, AVAILABILITY_TOPIC, AVAILABILITY_DLT, HOTELS_TOPIC, HOTELS_DLT));
    }

    @Test
    void shouldResolveAvailabilityDltOnSamePartitionWhenRecordComesFromAvailabilityTopic() {
        // given
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>(AVAILABILITY_TOPIC, 2, 15L, "7", new byte[0]);
        TopicPartition expectedDestination = new TopicPartition(AVAILABILITY_DLT, 2);

        // when
        TopicPartition destination = dltTopicResolver.resolve(record);

        // then
        assertThat(destination).isEqualTo(expectedDestination);
    }

    @Test
    void shouldResolveHotelsDltOnSamePartitionWhenRecordComesFromHotelsTopic() {
        // given
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>(HOTELS_TOPIC, 0, 3L, "7", new byte[0]);
        TopicPartition expectedDestination = new TopicPartition(HOTELS_DLT, 0);

        // when
        TopicPartition destination = dltTopicResolver.resolve(record);

        // then
        assertThat(destination).isEqualTo(expectedDestination);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenTopicHasNoDltMapping() {
        // given
        ConsumerRecord<Object, Object> record = new ConsumerRecord<>(BOOKINGS_TOPIC, 1, 0L, "7", new byte[0]);
        String expectedMessage = "No DLT mapping configured for topic: travel.bookings";

        // when & then
        assertThatThrownBy(() -> dltTopicResolver.resolve(record))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(expectedMessage);
    }
}
