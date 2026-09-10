package io.github.debeee.travelagency.command.application.service;

import io.github.debeee.travelagency.command.application.command.CreateBookingCommand;
import io.github.debeee.travelagency.command.application.exception.HotelNotFoundException;
import io.github.debeee.travelagency.command.application.port.in.CreateBookingUseCase;
import io.github.debeee.travelagency.command.application.port.out.AvailabilityRepository;
import io.github.debeee.travelagency.command.application.port.out.BookingRepository;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.domain.model.Booking;

public class BookingService implements CreateBookingUseCase {

    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;

    public BookingService(HotelRepository hotelRepository,
                          BookingRepository bookingRepository,
                          AvailabilityRepository availabilityRepository) {
        this.hotelRepository = hotelRepository;
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Override
    public Long createBooking(CreateBookingCommand command) {
        if (command.start().isAfter(command.end())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        var hotelId = command.hotelId();
        var hotel = hotelRepository.findHotel(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        availabilityRepository.reserveAvailability(
                hotel.getId(),
                hotel.getCapacity(),
                command.start(),
                command.end()
        );

        var newBooking = new Booking(null, hotelId, command.userId(), command.start(), command.end());
        var saved = bookingRepository.save(newBooking);

        return saved.id();
    }
}
