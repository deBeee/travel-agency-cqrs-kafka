package io.github.debeee.travelagency.query.application.port.out;

import io.github.debeee.travelagency.query.domain.model.Availability;

public interface AvailabilityWriteRepository {
    void upsert(Availability availability);
}
