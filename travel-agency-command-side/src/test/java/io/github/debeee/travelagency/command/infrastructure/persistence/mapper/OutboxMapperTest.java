package io.github.debeee.travelagency.command.infrastructure.persistence.mapper;

import io.github.debeee.travelagency.command.application.event.BookingCreatedPayload;
import io.github.debeee.travelagency.command.application.event.HotelUpsertedPayload;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.OutboxEntity;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;

class OutboxMapperTest {

    private final OutboxMapper outboxMapper = new OutboxMapper(new ObjectMapper());

    @Test
    void shouldSerializeBookingCreatedPayloadToJsonWhenMappingToOutboxEntity() {
        // given
        BookingCreatedPayload payload = new BookingCreatedPayload(
                42L, 7L, 100L, LocalDate.of(2027, 6, 1), LocalDate.of(2027, 6, 3));

        // when
        OutboxEntity entity = outboxMapper.toOutboxEntity(payload, "travel.bookings", "7", "BookingCreated");

        // then
        assertAll(
                () -> assertThat(entity.getId()).isNull(),
                () -> assertThat(entity.getAggregateId()).isEqualTo("7"),
                () -> assertThat(entity.getType()).isEqualTo("BookingCreated"),
                () -> assertThat(entity.getTopic()).isEqualTo("travel.bookings"),
                () -> assertThat(entity.getPayload())
                        .isEqualTo("{\"id\":42,\"hotelId\":7,\"userId\":100,\"start\":\"2027-06-01\",\"end\":\"2027-06-03\"}"),
                () -> assertThat(entity.getRetryCount()).isZero(),
                () -> assertThat(entity.getCreatedAt()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS))
        );
    }

    @Test
    void shouldSerializeHotelUpsertedPayloadToJsonWhenMappingToOutboxEntity() {
        // given
        HotelUpsertedPayload payload = new HotelUpsertedPayload(7L, 10L);

        // when
        OutboxEntity entity = outboxMapper.toOutboxEntity(payload, "travel.hotels", "7", "HotelUpserted");

        // then
        assertAll(
                () -> assertThat(entity.getId()).isNull(),
                () -> assertThat(entity.getAggregateId()).isEqualTo("7"),
                () -> assertThat(entity.getType()).isEqualTo("HotelUpserted"),
                () -> assertThat(entity.getTopic()).isEqualTo("travel.hotels"),
                () -> assertThat(entity.getPayload()).isEqualTo("{\"hotelId\":7,\"capacity\":10}"),
                () -> assertThat(entity.getRetryCount()).isZero(),
                () -> assertThat(entity.getCreatedAt()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS))
        );
    }
}
