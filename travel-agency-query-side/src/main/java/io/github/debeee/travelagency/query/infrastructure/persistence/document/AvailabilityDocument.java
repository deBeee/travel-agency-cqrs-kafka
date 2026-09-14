package io.github.debeee.travelagency.query.infrastructure.persistence.document;

import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Document(collection = "availability")
@CompoundIndex(name = "hotelId_date", def = "{'hotelId': 1, 'date': 1}")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDocument {

    @Id
    private String id;

    private long hotelId;
    private LocalDate date;
    private long occupied;
    private long capacity;
    private AvailabilityStatus status;
    private Instant updatedAt;
}
