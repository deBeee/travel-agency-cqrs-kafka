package io.github.debeee.travelagency.query.infrastructure.persistence.document;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityDocumentTest {

    @Test
    void shouldBuildDeterministicIdWhenHotelAndDateAreGiven() {
        // given
        long hotelId = 7L;
        LocalDate date = LocalDate.of(2027, 6, 1);
        String expectedId = "hotel_7_2027-06-01";

        // when
        String id = AvailabilityDocument.buildId(hotelId, date);

        // then
        assertThat(id).isEqualTo(expectedId);
    }
}
