package io.github.debeee.travelagency.command.application.service;

import io.github.debeee.travelagency.command.application.command.CreateBookingCommand;
import io.github.debeee.travelagency.command.application.exception.HotelNotFoundException;
import io.github.debeee.travelagency.command.application.port.in.CreateBookingUseCase;
import io.github.debeee.travelagency.command.application.port.out.BookingRepository;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.domain.model.Booking;

public class BookingService implements CreateBookingUseCase {

    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;

    public BookingService(HotelRepository hotelRepository, BookingRepository bookingRepository) {
        this.hotelRepository = hotelRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public Long createBooking(CreateBookingCommand command) {
        var hotelId = command.hotelId();
        var hotel = hotelRepository.findHotel(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        var saved = bookingRepository.save(
                new Booking(null, hotelId, command.userId(), command.start(), command.end())
        );

        return saved.id();
    }
}
