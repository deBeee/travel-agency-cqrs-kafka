package io.github.debeee.travelagency.query.infrastructure.persistence.adapter;

import io.github.debeee.travelagency.query.MongoContainerConfiguration;
import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import io.github.debeee.travelagency.query.infrastructure.persistence.document.AvailabilityDocument;
import io.github.debeee.travelagency.query.infrastructure.persistence.mapper.AvailabilityDocumentMapper;
import io.github.debeee.travelagency.query.infrastructure.persistence.repository.MongoDailyAvailabilityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataMongoTest
@Import({MongoContainerConfiguration.class, MongoAvailabilityRepositoryAdapter.class, AvailabilityDocumentMapper.class})
class MongoAvailabilityRepositoryAdapterIT {

    private static final long HOTEL_ID = 7L;
    private static final long OTHER_HOTEL_ID = 8L;
    private static final LocalDate JUNE_1 = LocalDate.of(2027, 6, 1);
    private static final LocalDate JUNE_2 = LocalDate.of(2027, 6, 2);
    private static final LocalDate JUNE_3 = LocalDate.of(2027, 6, 3);
    private static final LocalDate JUNE_5 = LocalDate.of(2027, 6, 5);

    @Autowired
    private MongoAvailabilityRepositoryAdapter adapter;

    @Autowired
    private MongoDailyAvailabilityRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void shouldInsertDocumentWithDeterministicIdWhenDayIsProjectedForTheFirstTime() {
        // given
        Availability availability = new Availability(HOTEL_ID, JUNE_1, 3, 10, AvailabilityStatus.AVAILABLE);
        String expectedId = "hotel_7_2027-06-01";

        // when
        adapter.upsert(availability);

        // then
        AvailabilityDocument document = repository.findById(expectedId).orElseThrow();
        assertAll(
                () -> assertThat(repository.count()).isEqualTo(1),
                () -> assertThat(document.getHotelId()).isEqualTo(HOTEL_ID),
                () -> assertThat(document.getDate()).isEqualTo(JUNE_1),
                () -> assertThat(document.getOccupied()).isEqualTo(3),
                () -> assertThat(document.getCapacity()).isEqualTo(10),
                () -> assertThat(document.getStatus()).isEqualTo(AvailabilityStatus.AVAILABLE),
                () -> assertThat(document.getUpdatedAt()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS))
        );
    }

    @Test
    void shouldUpdateExistingDocumentInsteadOfInsertingWhenSameDayIsProjectedAgain() {
        // given
        adapter.upsert(new Availability(HOTEL_ID, JUNE_1, 3, 10, AvailabilityStatus.AVAILABLE));
        Availability newerAvailability = new Availability(HOTEL_ID, JUNE_1, 10, 10, AvailabilityStatus.SOLD_OUT);
        String expectedId = "hotel_7_2027-06-01";

        // when
        adapter.upsert(newerAvailability);

        // then
        AvailabilityDocument document = repository.findById(expectedId).orElseThrow();
        assertAll(
                () -> assertThat(repository.count()).isEqualTo(1),
                () -> assertThat(document.getOccupied()).isEqualTo(10),
                () -> assertThat(document.getStatus()).isEqualTo(AvailabilityStatus.SOLD_OUT)
        );
    }

    @Test
    void shouldLeaveReadModelUnchangedWhenSameEventIsReplayed() {
        // given
        Availability availability = new Availability(HOTEL_ID, JUNE_1, 3, 10, AvailabilityStatus.AVAILABLE);
        adapter.upsert(availability);
        List<Availability> expectedAvailabilities = List.of(availability);

        // when
        adapter.upsert(availability);
        adapter.upsert(availability);

        // then
        assertAll(
                () -> assertThat(repository.count()).isEqualTo(1),
                () -> assertThat(adapter.findByHotel(HOTEL_ID, null, null)).isEqualTo(expectedAvailabilities)
        );
    }

    @Test
    void shouldReturnDaysWithinRangeOrderedByDateWhenRangeIsGiven() {
        // given
        adapter.upsert(new Availability(HOTEL_ID, JUNE_3, 1, 10, AvailabilityStatus.AVAILABLE));
        adapter.upsert(new Availability(HOTEL_ID, JUNE_1, 2, 10, AvailabilityStatus.AVAILABLE));
        adapter.upsert(new Availability(HOTEL_ID, JUNE_5, 3, 10, AvailabilityStatus.AVAILABLE));
        adapter.upsert(new Availability(HOTEL_ID, JUNE_2, 10, 10, AvailabilityStatus.SOLD_OUT));
        adapter.upsert(new Availability(OTHER_HOTEL_ID, JUNE_2, 4, 10, AvailabilityStatus.AVAILABLE));
        List<Availability> expectedAvailabilities = List.of(
                new Availability(HOTEL_ID, JUNE_1, 2, 10, AvailabilityStatus.AVAILABLE),
                new Availability(HOTEL_ID, JUNE_2, 10, 10, AvailabilityStatus.SOLD_OUT),
                new Availability(HOTEL_ID, JUNE_3, 1, 10, AvailabilityStatus.AVAILABLE));

        // when
        List<Availability> availabilities = adapter.findByHotel(HOTEL_ID, JUNE_1, JUNE_3);

        // then
        assertThat(availabilities).containsExactlyElementsOf(expectedAvailabilities);
    }

    @Test
    void shouldReturnAllDaysOfHotelOrderedByDateWhenRangeIsNotGiven() {
        // given
        adapter.upsert(new Availability(HOTEL_ID, JUNE_5, 3, 10, AvailabilityStatus.AVAILABLE));
        adapter.upsert(new Availability(HOTEL_ID, JUNE_1, 2, 10, AvailabilityStatus.AVAILABLE));
        adapter.upsert(new Availability(OTHER_HOTEL_ID, JUNE_2, 4, 10, AvailabilityStatus.AVAILABLE));
        List<Availability> expectedAvailabilities = List.of(
                new Availability(HOTEL_ID, JUNE_1, 2, 10, AvailabilityStatus.AVAILABLE),
                new Availability(HOTEL_ID, JUNE_5, 3, 10, AvailabilityStatus.AVAILABLE));

        // when
        List<Availability> availabilities = adapter.findByHotel(HOTEL_ID, null, null);

        // then
        assertThat(availabilities).containsExactlyElementsOf(expectedAvailabilities);
    }

    @Test
    void shouldReturnEmptyListWhenHotelHasNoProjectedDays() {
        // given
        adapter.upsert(new Availability(OTHER_HOTEL_ID, JUNE_1, 4, 10, AvailabilityStatus.AVAILABLE));

        // when
        List<Availability> availabilities = adapter.findByHotel(HOTEL_ID, JUNE_1, JUNE_3);

        // then
        assertThat(availabilities).isEmpty();
    }
}
