package io.github.debeee.travelagency.query.infrastructure.persistence.repository;

import io.github.debeee.travelagency.query.infrastructure.persistence.document.AvailabilityDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface MongoDailyAvailabilityRepository extends MongoRepository<AvailabilityDocument, String> {
    List<AvailabilityDocument> findByHotelIdOrderByDateAsc(long hotelId);

    @Query(value = "{ 'hotelId': ?0, 'date': { $gte: ?1, $lte: ?2 } }", sort = "{ 'date': 1 }")
    List<AvailabilityDocument> findByHotelIdAndDateRange(long hotelId, LocalDate from, LocalDate to);
}
