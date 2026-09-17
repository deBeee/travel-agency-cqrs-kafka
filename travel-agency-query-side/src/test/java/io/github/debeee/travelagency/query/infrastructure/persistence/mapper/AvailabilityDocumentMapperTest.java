package io.github.debeee.travelagency.query.infrastructure.persistence.mapper;

import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import io.github.debeee.travelagency.query.infrastructure.persistence.document.AvailabilityDocument;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityDocumentMapperTest {

    private final AvailabilityDocumentMapper mapper = new AvailabilityDocumentMapper();

    @Test
    void shouldMapDocumentFieldsToDomainWhenDocumentIsComplete() {
        // given
        LocalDate date = LocalDate.of(2027, 6, 1);
        AvailabilityDocument document = AvailabilityDocument.builder()
                .id(AvailabilityDocument.buildId(7L, date))
                .hotelId(7L)
                .date(date)
                .occupied(3)
                .capacity(10)
                .status(AvailabilityStatus.AVAILABLE)
                .updatedAt(Instant.parse("2027-05-01T10:00:00Z"))
                .build();
        Availability expectedAvailability = new Availability(7L, date, 3, 10, AvailabilityStatus.AVAILABLE);

        // when
        Availability availability = mapper.toDomain(document);

        // then
        assertThat(availability).isEqualTo(expectedAvailability);
    }
}
