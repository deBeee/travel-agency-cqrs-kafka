package io.github.debeee.travelagency.query.infrastructure.persistence.adapter;

import io.github.debeee.travelagency.query.application.port.out.AvailabilityReadRepository;
import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.infrastructure.persistence.document.AvailabilityDocument;
import io.github.debeee.travelagency.query.infrastructure.persistence.mapper.AvailabilityDocumentMapper;
import io.github.debeee.travelagency.query.infrastructure.persistence.repository.MongoDailyAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MongoAvailabilityAdapter implements AvailabilityReadRepository {

    private final MongoDailyAvailabilityRepository repository;
    private final AvailabilityDocumentMapper mapper;

    @Override
    public List<Availability> findByHotel(long hotelId, LocalDate from, LocalDate to) {
        List<AvailabilityDocument> docs = (from != null && to != null)
                ? repository.findByHotelIdAndDateRange(hotelId, from, to)
                : repository.findByHotelIdOrderByDateAsc(hotelId);

        return docs.stream()
                .map(mapper::toDomain)
                .toList();
    }
}
