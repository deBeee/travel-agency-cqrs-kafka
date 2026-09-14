package io.github.debeee.travelagency.query.infrastructure.persistence.mapper;

import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.infrastructure.persistence.document.AvailabilityDocument;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityDocumentMapper {
    
    public Availability toDomain(AvailabilityDocument doc) {
        return new Availability(
                doc.getHotelId(),
                doc.getDate(),
                doc.getOccupied(),
                doc.getCapacity(),
                doc.getStatus()
        );
    }
}
