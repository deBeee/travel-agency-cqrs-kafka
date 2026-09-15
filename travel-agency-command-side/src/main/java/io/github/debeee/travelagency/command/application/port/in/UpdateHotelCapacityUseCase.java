package io.github.debeee.travelagency.command.application.port.in;

public interface UpdateHotelCapacityUseCase {
    void updateCapacity(Long hotelId, long capacity);
}
