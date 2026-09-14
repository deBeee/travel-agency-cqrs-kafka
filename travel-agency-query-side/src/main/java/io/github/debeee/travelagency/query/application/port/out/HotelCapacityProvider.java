package io.github.debeee.travelagency.query.application.port.out;

public interface HotelCapacityProvider {
    long getCapacity(long hotelId);
}
