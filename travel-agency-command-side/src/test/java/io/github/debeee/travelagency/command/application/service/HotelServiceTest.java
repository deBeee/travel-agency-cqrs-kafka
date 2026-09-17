package io.github.debeee.travelagency.command.application.service;

import io.github.debeee.travelagency.command.application.exception.HotelNotFoundException;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.application.port.out.OutboxRepository;
import io.github.debeee.travelagency.command.domain.model.Hotel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    private static final Long HOTEL_ID = 5L;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private OutboxRepository<Hotel> hotelOutboxRepository;

    @InjectMocks
    private HotelService hotelService;

    @Test
    void shouldReturnGeneratedIdWhenHotelIsCreated() {
        // given
        long capacity = 10;
        Hotel newHotel = new Hotel(null, capacity);
        Hotel savedHotel = new Hotel(HOTEL_ID, capacity);
        given(hotelRepository.saveHotel(newHotel)).willReturn(savedHotel);

        // when
        Long hotelId = hotelService.createHotel(capacity);

        // then
        assertThat(hotelId).isEqualTo(savedHotel.id());
    }

    @Test
    void shouldSaveHotelWithoutIdThenWriteItToOutboxWhenHotelIsCreated() {
        // given
        long capacity = 10;
        Hotel newHotel = new Hotel(null, capacity);
        Hotel savedHotel = new Hotel(HOTEL_ID, capacity);
        given(hotelRepository.saveHotel(newHotel)).willReturn(savedHotel);
        InOrder inOrder = inOrder(hotelRepository, hotelOutboxRepository);

        // when
        hotelService.createHotel(capacity);

        // then
        then(hotelRepository).should(inOrder).saveHotel(newHotel);
        then(hotelOutboxRepository).should(inOrder).saveOutbox(savedHotel);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenCreatedHotelCapacityIsNotPositive() {
        // given
        long capacity = 0;
        String expectedMessage = "Capacity must be positive";

        // when & then
        assertThatThrownBy(() -> hotelService.createHotel(capacity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        then(hotelRepository).shouldHaveNoInteractions();
        then(hotelOutboxRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldSaveHotelWithNewCapacityThenWriteItToOutboxWhenHotelExists() {
        // given
        long newCapacity = 20;
        Hotel existingHotel = new Hotel(HOTEL_ID, 10);
        Hotel updatedHotel = new Hotel(HOTEL_ID, newCapacity);
        given(hotelRepository.findHotel(HOTEL_ID)).willReturn(Optional.of(existingHotel));
        given(hotelRepository.saveHotel(updatedHotel)).willReturn(updatedHotel);
        InOrder inOrder = inOrder(hotelRepository, hotelOutboxRepository);

        // when
        hotelService.updateCapacity(HOTEL_ID, newCapacity);

        // then
        then(hotelRepository).should(inOrder).saveHotel(updatedHotel);
        then(hotelOutboxRepository).should(inOrder).saveOutbox(updatedHotel);
    }

    @Test
    void shouldThrowHotelNotFoundExceptionWhenUpdatedHotelDoesNotExist() {
        // given
        long newCapacity = 20;
        given(hotelRepository.findHotel(HOTEL_ID)).willReturn(Optional.empty());
        String expectedMessage = "Hotel 5 not found";

        // when & then
        assertThatThrownBy(() -> hotelService.updateCapacity(HOTEL_ID, newCapacity))
                .isInstanceOf(HotelNotFoundException.class)
                .hasMessage(expectedMessage);
        then(hotelRepository).should().findHotel(HOTEL_ID);
        then(hotelRepository).shouldHaveNoMoreInteractions();
        then(hotelOutboxRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenUpdatedCapacityIsNotPositive() {
        // given
        Hotel existingHotel = new Hotel(HOTEL_ID, 10);
        given(hotelRepository.findHotel(HOTEL_ID)).willReturn(Optional.of(existingHotel));
        String expectedMessage = "Capacity must be positive";

        // when & then
        assertThatThrownBy(() -> hotelService.updateCapacity(HOTEL_ID, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
        then(hotelRepository).should().findHotel(HOTEL_ID);
        then(hotelRepository).shouldHaveNoMoreInteractions();
        then(hotelOutboxRepository).shouldHaveNoInteractions();
    }
}
