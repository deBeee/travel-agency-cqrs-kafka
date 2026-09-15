package io.github.debeee.travelagency.command.presentation.controller;

import io.github.debeee.travelagency.command.application.port.in.CreateHotelUseCase;
import io.github.debeee.travelagency.command.application.port.in.UpdateHotelCapacityUseCase;
import io.github.debeee.travelagency.command.presentation.dto.hotel.CreateHotelRequestDto;
import io.github.debeee.travelagency.command.presentation.dto.hotel.CreateHotelResponseDto;
import io.github.debeee.travelagency.command.presentation.dto.hotel.UpdateHotelCapacityRequestDto;
import io.github.debeee.travelagency.command.presentation.dto.hotel.UpdateHotelCapacityResponseDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final CreateHotelUseCase createHotelUseCase;
    private final UpdateHotelCapacityUseCase updateHotelCapacityUseCase;

    public HotelController(
            @Qualifier("transactionalCreateHotelUseCase") CreateHotelUseCase createHotelUseCase,
            @Qualifier("transactionalUpdateHotelCapacityUseCase") UpdateHotelCapacityUseCase updateHotelCapacityUseCase) {
        this.createHotelUseCase = createHotelUseCase;
        this.updateHotelCapacityUseCase = updateHotelCapacityUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateHotelResponseDto> createHotel(@RequestBody @Valid CreateHotelRequestDto request) {
        Long hotelId = createHotelUseCase.createHotel(request.capacity());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new CreateHotelResponseDto(hotelId));
    }

    @PutMapping("/{hotelId}")
    public ResponseEntity<UpdateHotelCapacityResponseDto> updateHotelCapacity(
            @PathVariable Long hotelId,
            @RequestBody @Valid UpdateHotelCapacityRequestDto request) {
        updateHotelCapacityUseCase.updateCapacity(hotelId, request.capacity());
        return ResponseEntity
                .ok(new UpdateHotelCapacityResponseDto(hotelId, request.capacity()));
    }
}
