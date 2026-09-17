package io.github.debeee.travelagency.query.infrastructure.capacity;

import io.github.debeee.travelagency.query.application.port.out.HotelCapacityProvider;
import io.github.debeee.travelagency.query.infrastructure.persistence.document.HotelDocument;
import io.github.debeee.travelagency.query.infrastructure.persistence.repository.MongoHotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

@ExtendWith(MockitoExtension.class)
class MongoHotelCapacityProviderTest {

    private static final long HOTEL_ID = 7L;

    @Mock
    private MongoHotelRepository hotelRepository;

    @Mock
    private HotelCapacityProvider fallback;

    private MongoHotelCapacityProvider capacityProvider;

    @BeforeEach
    void setUp() {
        capacityProvider = new MongoHotelCapacityProvider(hotelRepository, fallback);
    }

    @Test
    void shouldReturnCapacityFromDocumentWhenHotelIsInReadModel() {
        // given
        given(hotelRepository.findById(HOTEL_ID)).willReturn(Optional.of(HotelDocument.builder().id(HOTEL_ID).capacity(10).build()));
        long expectedCapacity = 10;

        // when
        long capacity = capacityProvider.getCapacity(HOTEL_ID);

        // then
        assertThat(capacity).isEqualTo(expectedCapacity);
        then(fallback).shouldHaveNoInteractions();
    }

    @Test
    void shouldReadDocumentOnlyOnceWhenCapacityIsRequestedRepeatedly() {
        // given
        given(hotelRepository.findById(HOTEL_ID)).willReturn(Optional.of(HotelDocument.builder().id(HOTEL_ID).capacity(10).build()));
        long expectedCapacity = 10;

        // when
        long firstCapacity = capacityProvider.getCapacity(HOTEL_ID);
        long secondCapacity = capacityProvider.getCapacity(HOTEL_ID);

        // then
        assertAll(
                () -> assertThat(firstCapacity).isEqualTo(expectedCapacity),
                () -> assertThat(secondCapacity).isEqualTo(expectedCapacity)
        );
        then(hotelRepository).should(times(1)).findById(HOTEL_ID);
    }

    @Test
    void shouldReturnFallbackCapacityWithoutCachingItWhenHotelIsNotInReadModel() {
        // given
        given(hotelRepository.findById(HOTEL_ID)).willReturn(Optional.empty());
        given(fallback.getCapacity(HOTEL_ID)).willReturn(100L);
        long expectedCapacity = 100;

        // when
        long firstCapacity = capacityProvider.getCapacity(HOTEL_ID);
        long secondCapacity = capacityProvider.getCapacity(HOTEL_ID);

        // then
        assertAll(
                () -> assertThat(firstCapacity).isEqualTo(expectedCapacity),
                () -> assertThat(secondCapacity).isEqualTo(expectedCapacity)
        );
        then(hotelRepository).should(times(2)).findById(HOTEL_ID);
    }

    @Test
    void shouldSaveDocumentAndServeCapacityFromCacheWhenCapacityIsSaved() {
        // given
        long capacity = 20;
        ArgumentCaptor<HotelDocument> documentCaptor = ArgumentCaptor.forClass(HotelDocument.class);

        // when
        capacityProvider.save(HOTEL_ID, capacity);
        long cachedCapacity = capacityProvider.getCapacity(HOTEL_ID);

        // then
        then(hotelRepository).should().save(documentCaptor.capture());
        then(hotelRepository).shouldHaveNoMoreInteractions();
        then(fallback).shouldHaveNoInteractions();
        HotelDocument savedDocument = documentCaptor.getValue();
        assertAll(
                () -> assertThat(savedDocument.getId()).isEqualTo(HOTEL_ID),
                () -> assertThat(savedDocument.getCapacity()).isEqualTo(capacity),
                () -> assertThat(cachedCapacity).isEqualTo(capacity)
        );
    }

    @Test
    void shouldReplaceCachedCapacityWhenCapacityIsSavedAgain() {
        // given
        long newCapacity = 30;
        capacityProvider.save(HOTEL_ID, 20);

        // when
        capacityProvider.save(HOTEL_ID, newCapacity);
        long cachedCapacity = capacityProvider.getCapacity(HOTEL_ID);

        // then
        assertThat(cachedCapacity).isEqualTo(newCapacity);
    }
}
