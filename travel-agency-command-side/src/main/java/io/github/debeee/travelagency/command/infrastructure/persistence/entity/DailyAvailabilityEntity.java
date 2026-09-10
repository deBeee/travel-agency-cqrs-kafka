package io.github.debeee.travelagency.command.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "daily_availabilities")
@IdClass(DailyAvailabilityId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyAvailabilityEntity {

    @Id
    @Column(name = "hotel_id")
    private Long hotelId;

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "occupied_rooms")
    private long occupiedRooms;
}
