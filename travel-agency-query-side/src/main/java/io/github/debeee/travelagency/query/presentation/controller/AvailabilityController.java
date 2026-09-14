package io.github.debeee.travelagency.query.presentation.controller;

import io.github.debeee.travelagency.query.application.port.in.GetAvailabilityUseCase;
import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.presentation.dto.AvailabilityResponseDto;
import io.github.debeee.travelagency.query.presentation.mapper.AvailabilityResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final GetAvailabilityUseCase getAvailabilityUseCase;
    private final AvailabilityResponseMapper availabilityResponseMapper;
    
    @GetMapping("/{hotelId}")
    public ResponseEntity<List<AvailabilityResponseDto>> getAvailability(
            @PathVariable long hotelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        if ((from == null) != (to == null)) {
            throw new IllegalArgumentException("Parameters from and to must be provided together");
        }

        if (from != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Parameter from cannot be after to");
        }

        List<Availability> result = getAvailabilityUseCase.getForHotel(hotelId, from, to);
        return ResponseEntity.ok(availabilityResponseMapper.toAvailabilityResponseDtos(result));
    }
}
