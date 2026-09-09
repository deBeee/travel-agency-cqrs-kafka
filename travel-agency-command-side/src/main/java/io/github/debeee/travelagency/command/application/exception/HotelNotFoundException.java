package io.github.debeee.travelagency.command.application.exception;

public class HotelNotFoundException extends ResourceNotFoundException {

    private final Long hotelId;

    public HotelNotFoundException(Long hotelId) {
        super("Hotel %d not found".formatted(hotelId));
        this.hotelId = hotelId;
    }

    public Long getHotelId() {
        return hotelId;
    }
}