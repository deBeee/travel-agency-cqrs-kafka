package io.github.debeee.travelagency.query.application.port.in;

public interface UpsertHotelCapacityUseCase {
    void upsert(long hotelId, long capacity);
}
