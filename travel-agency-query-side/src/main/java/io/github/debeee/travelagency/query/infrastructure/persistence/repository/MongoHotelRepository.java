package io.github.debeee.travelagency.query.infrastructure.persistence.repository;

import io.github.debeee.travelagency.query.infrastructure.persistence.document.HotelDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MongoHotelRepository extends MongoRepository<HotelDocument, Long> {
}
