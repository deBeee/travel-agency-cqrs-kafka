package io.github.debeee.travelagency.command.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyAvailabilityId implements Serializable {
    private Long hotelId;
    private LocalDate date;
}

