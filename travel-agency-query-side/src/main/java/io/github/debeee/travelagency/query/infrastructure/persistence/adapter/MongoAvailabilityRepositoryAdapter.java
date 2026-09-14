package io.github.debeee.travelagency.query.infrastructure.persistence.adapter;

import io.github.debeee.travelagency.query.application.port.out.AvailabilityReadRepository;
import io.github.debeee.travelagency.query.application.port.out.AvailabilityWriteRepository;
import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.infrastructure.persistence.document.AvailabilityDocument;
import io.github.debeee.travelagency.query.infrastructure.persistence.mapper.AvailabilityDocumentMapper;
import io.github.debeee.travelagency.query.infrastructure.persistence.repository.MongoDailyAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MongoAvailabilityRepositoryAdapter implements AvailabilityReadRepository, AvailabilityWriteRepository {

    private final MongoDailyAvailabilityRepository repository;
    private final AvailabilityDocumentMapper mapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Availability> findByHotel(long hotelId, LocalDate from, LocalDate to) {
        List<AvailabilityDocument> docs = (from != null && to != null)
                ? repository.findByHotelIdAndDateRange(hotelId, from, to)
                : repository.findByHotelIdOrderByDateAsc(hotelId);

        return docs.stream()
                .map(mapper::toDomain)
                .toList();
    }
    
    @Override
    public void upsert(Availability availability) {
        String id = AvailabilityDocument.buildId(availability.getHotelId(), availability.getDate());
        Query query = new Query(Criteria.where("_id").is(id));

        Update update = new Update()
                .set("hotelId", availability.getHotelId())
                .set("date", availability.getDate())
                .set("occupied", availability.getOccupied())
                .set("capacity", availability.getCapacity())
                .set("status", availability.getStatus())
                .set("updatedAt", Instant.now());

        mongoTemplate.upsert(query, update, AvailabilityDocument.class);
    }
}
