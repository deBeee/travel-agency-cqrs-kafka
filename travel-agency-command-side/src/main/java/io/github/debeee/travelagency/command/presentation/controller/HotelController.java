package io.github.debeee.travelagency.command.presentation.controller;

import io.github.debeee.travelagency.command.application.port.in.CreateHotelUseCase;
import io.github.debeee.travelagency.command.presentation.dto.hotel.CreateHotelRequestDto;
import io.github.debeee.travelagency.command.presentation.dto.hotel.CreateHotelResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final CreateHotelUseCase createHotelUseCase;

    public HotelController(@Qualifier("transactionalCreateHotelUseCase") CreateHotelUseCase createHotelUseCase) {
        this.createHotelUseCase = createHotelUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateHotelResponseDto> createHotel(@RequestBody @Valid CreateHotelRequestDto request) {
        Long hotelId = createHotelUseCase.createHotel(request.capacity());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateHotelResponseDto(hotelId));
    }
}
