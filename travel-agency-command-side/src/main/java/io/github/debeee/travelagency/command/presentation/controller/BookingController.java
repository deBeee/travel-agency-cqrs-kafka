package io.github.debeee.travelagency.command.presentation.controller;

import io.github.debeee.travelagency.command.application.command.CreateBookingCommand;
import io.github.debeee.travelagency.command.application.port.in.CreateBookingUseCase;
import io.github.debeee.travelagency.command.presentation.dto.booking.CreateBookingRequestDto;
import io.github.debeee.travelagency.command.presentation.dto.booking.CreateBookingResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;

    public BookingController(@Qualifier("retryingCreateBookingUseCase") CreateBookingUseCase createBookingUseCase) {
        this.createBookingUseCase = createBookingUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateBookingResponseDto> createBooking(@RequestBody @Valid CreateBookingRequestDto request) {
        var command = new CreateBookingCommand(request.hotelId(), request.userId(), request.start(), request.end());
        var bookingId = createBookingUseCase.createBooking(command);
        return ResponseEntity
                .status(CREATED)
                .body(new CreateBookingResponseDto(bookingId));
    }
}
