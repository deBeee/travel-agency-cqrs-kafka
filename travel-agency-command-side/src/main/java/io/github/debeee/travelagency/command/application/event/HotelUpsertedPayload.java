package io.github.debeee.travelagency.command.application.event;

public record HotelUpsertedPayload(Long hotelId, Long capacity) implements OutboxPayload {
}
