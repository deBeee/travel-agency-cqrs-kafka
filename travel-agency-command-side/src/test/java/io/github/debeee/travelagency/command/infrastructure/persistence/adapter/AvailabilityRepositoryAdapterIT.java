package io.github.debeee.travelagency.command.infrastructure.persistence.adapter;

import io.github.debeee.travelagency.command.MySqlContainerConfiguration;
import io.github.debeee.travelagency.command.domain.exception.OverbookingException;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.DailyAvailabilityEntity;
import io.github.debeee.travelagency.command.infrastructure.persistence.mapper.AvailabilityMapper;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaDailyAvailabilityRepository;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@Import({MySqlContainerConfiguration.class, AvailabilityRepositoryAdapter.class, AvailabilityMapper.class})
class AvailabilityRepositoryAdapterIT {

    private static final Long HOTEL_ID = 7L;
    private static final Long OTHER_HOTEL_ID = 8L;
    private static final long CAPACITY = 2;
    private static final LocalDate JUNE_1 = LocalDate.of(2027, 6, 1);
    private static final LocalDate JUNE_2 = LocalDate.of(2027, 6, 2);
    private static final LocalDate JUNE_3 = LocalDate.of(2027, 6, 3);

    @Autowired
    private AvailabilityRepositoryAdapter availabilityRepositoryAdapter;

    @Autowired
    private JpaDailyAvailabilityRepository jpaDailyAvailabilityRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void cleanUp() {
        jpaDailyAvailabilityRepository.deleteAll();
    }

    @Test
    void shouldCreateOneRowPerNightWithOneOccupiedRoomWhenNoAvailabilityExists() {
        // given
        List<Tuple> expectedRows = List.of(
                tuple(HOTEL_ID, JUNE_1, 1L),
                tuple(HOTEL_ID, JUNE_2, 1L),
                tuple(HOTEL_ID, JUNE_3, 1L));

        // when
        availabilityRepositoryAdapter.reserveAvailability(HOTEL_ID, CAPACITY, JUNE_1, JUNE_3);

        // then
        entityManager.flush();
        entityManager.clear();
        List<DailyAvailabilityEntity> rows = jpaDailyAvailabilityRepository.findAll();
        assertThat(rows)
                .extracting(DailyAvailabilityEntity::getHotelId, DailyAvailabilityEntity::getDate, DailyAvailabilityEntity::getOccupiedRooms)
                .containsExactlyInAnyOrderElementsOf(expectedRows);
    }

    @Test
    void shouldIncrementExistingNightsAndCreateMissingOnesWhenRangeIsPartiallyOccupied() {
        // given
        entityManager.persistAndFlush(new DailyAvailabilityEntity(HOTEL_ID, JUNE_1, 1));
        entityManager.clear();
        List<Tuple> expectedRows = List.of(
                tuple(JUNE_1, 2L),
                tuple(JUNE_2, 1L));

        // when
        availabilityRepositoryAdapter.reserveAvailability(HOTEL_ID, CAPACITY, JUNE_1, JUNE_2);

        // then
        entityManager.flush();
        entityManager.clear();
        List<DailyAvailabilityEntity> rows = jpaDailyAvailabilityRepository.findAll();
        assertThat(rows)
                .extracting(DailyAvailabilityEntity::getDate, DailyAvailabilityEntity::getOccupiedRooms)
                .containsExactlyInAnyOrderElementsOf(expectedRows);
    }

    @Test
    void shouldLeaveOtherHotelsUntouchedWhenReservingForOneHotel() {
        // given
        entityManager.persistAndFlush(new DailyAvailabilityEntity(OTHER_HOTEL_ID, JUNE_1, 1));
        entityManager.clear();
        List<Tuple> expectedRows = List.of(
                tuple(OTHER_HOTEL_ID, 1L),
                tuple(HOTEL_ID, 1L));

        // when
        availabilityRepositoryAdapter.reserveAvailability(HOTEL_ID, CAPACITY, JUNE_1, JUNE_1);

        // then
        entityManager.flush();
        entityManager.clear();
        List<DailyAvailabilityEntity> rows = jpaDailyAvailabilityRepository.findAll();
        assertThat(rows)
                .extracting(DailyAvailabilityEntity::getHotelId, DailyAvailabilityEntity::getOccupiedRooms)
                .containsExactlyInAnyOrderElementsOf(expectedRows);
    }

    @Test
    void shouldThrowOverbookingExceptionAndPersistNothingWhenAnyNightInRangeIsFull() {
        // given
        entityManager.persistAndFlush(new DailyAvailabilityEntity(HOTEL_ID, JUNE_2, CAPACITY));
        entityManager.clear();
        String expectedMessage = "Hotel 7 overbooked on 2027-06-02. Capacity: 2, occupied: 2";
        List<Tuple> expectedRows = List.of(tuple(JUNE_2, CAPACITY));

        // when & then
        assertThatThrownBy(() -> availabilityRepositoryAdapter.reserveAvailability(HOTEL_ID, CAPACITY, JUNE_1, JUNE_3))
                .isInstanceOf(OverbookingException.class)
                .hasMessage(expectedMessage);
        entityManager.flush();
        entityManager.clear();
        List<DailyAvailabilityEntity> rows = jpaDailyAvailabilityRepository.findAll();
        assertThat(rows)
                .extracting(DailyAvailabilityEntity::getDate, DailyAvailabilityEntity::getOccupiedRooms)
                .containsExactlyElementsOf(expectedRows);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRejectSecondTransactionWhenTwoTransactionsReserveLastRoomConcurrently() throws Exception {
        // given
        TransactionTemplate transaction = readCommittedTransaction();
        transaction.executeWithoutResult(status ->
                jpaDailyAvailabilityRepository.save(new DailyAvailabilityEntity(HOTEL_ID, JUNE_1, CAPACITY - 1)));
        CountDownLatch firstTransactionHoldsLock = new CountDownLatch(1);
        CountDownLatch secondTransactionWaitsForLock = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        long expectedOccupiedRooms = CAPACITY;

        // when
        Future<Throwable> firstOutcome = executor.submit(() -> catchThrowable(() ->
                transaction.executeWithoutResult(status -> {
                    availabilityRepositoryAdapter.reserveAvailability(HOTEL_ID, CAPACITY, JUNE_1, JUNE_1);
                    firstTransactionHoldsLock.countDown();
                    awaitQuietly(secondTransactionWaitsForLock);
                })));
        Future<Throwable> secondOutcome = executor.submit(() -> catchThrowable(() ->
                transaction.executeWithoutResult(status -> {
                    awaitQuietly(firstTransactionHoldsLock);
                    secondTransactionWaitsForLock.countDown();
                    availabilityRepositoryAdapter.reserveAvailability(HOTEL_ID, CAPACITY, JUNE_1, JUNE_1);
                })));
        Throwable firstResult = firstOutcome.get();
        Throwable secondResult = secondOutcome.get();
        executor.shutdown();

        // then
        DailyAvailabilityEntity row = jpaDailyAvailabilityRepository.findAll().getFirst();
        assertAll(
                () -> assertThat(firstResult).isNull(),
                () -> assertThat(secondResult).isInstanceOf(OverbookingException.class),
                () -> assertThat(row.getOccupiedRooms()).isEqualTo(expectedOccupiedRooms)
        );
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldThrowDataIntegrityViolationExceptionWhenTwoTransactionsInsertSameNightConcurrently() throws Exception {
        // given
        TransactionTemplate transaction = readCommittedTransaction();
        CyclicBarrier bothSelectedNothing = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        int expectedFailureCount = 1;
        long expectedOccupiedRooms = 1;

        // when
        Future<Throwable> firstOutcome = executor.submit(() -> catchThrowable(() ->
                transaction.executeWithoutResult(status -> {
                    availabilityRepositoryAdapter.reserveAvailability(HOTEL_ID, CAPACITY, JUNE_1, JUNE_1);
                    awaitQuietly(bothSelectedNothing);
                })));
        Future<Throwable> secondOutcome = executor.submit(() -> catchThrowable(() ->
                transaction.executeWithoutResult(status -> {
                    availabilityRepositoryAdapter.reserveAvailability(HOTEL_ID, CAPACITY, JUNE_1, JUNE_1);
                    awaitQuietly(bothSelectedNothing);
                })));
        List<Throwable> failures = Stream.of(firstOutcome.get(), secondOutcome.get())
                .filter(Objects::nonNull)
                .toList();
        executor.shutdown();

        // then
        DailyAvailabilityEntity row = jpaDailyAvailabilityRepository.findAll().getFirst();
        assertAll(
                () -> assertThat(failures).hasSize(expectedFailureCount),
                () -> assertThat(failures.getFirst()).isInstanceOf(DataIntegrityViolationException.class),
                () -> assertThat(row.getOccupiedRooms()).isEqualTo(expectedOccupiedRooms)
        );
    }

    private TransactionTemplate readCommittedTransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return template;
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void awaitQuietly(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
