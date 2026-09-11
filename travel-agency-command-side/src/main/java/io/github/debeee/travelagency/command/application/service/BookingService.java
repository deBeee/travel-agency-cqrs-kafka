package io.github.debeee.travelagency.command.application.service;

import io.github.debeee.travelagency.command.application.command.CreateBookingCommand;
import io.github.debeee.travelagency.command.application.exception.HotelNotFoundException;
import io.github.debeee.travelagency.command.application.port.in.CreateBookingUseCase;
import io.github.debeee.travelagency.command.application.port.out.AvailabilityRepository;
import io.github.debeee.travelagency.command.application.port.out.BookingRepository;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.application.port.out.OutboxRepository;
import io.github.debeee.travelagency.command.domain.model.Booking;

public class BookingService implements CreateBookingUseCase {

    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;
    private final OutboxRepository<Booking> bookingOutboxRepository;

    public BookingService(HotelRepository hotelRepository,
                          BookingRepository bookingRepository,
                          AvailabilityRepository availabilityRepository,
                          OutboxRepository<Booking> bookingOutboxRepository) {
        this.hotelRepository = hotelRepository;
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
        this.bookingOutboxRepository = bookingOutboxRepository;
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

        bookingOutboxRepository.saveOutbox(saved);

        return saved.id();
    }
}
