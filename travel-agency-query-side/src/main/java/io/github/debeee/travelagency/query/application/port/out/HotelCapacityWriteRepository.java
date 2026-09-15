package io.github.debeee.travelagency.query.application.port.out;

public interface HotelCapacityWriteRepository {
    void save(long hotelId, long capacity);
}
