package io.github.debeee.travelagency.command.application.service;

import io.github.debeee.travelagency.command.application.command.CreateBookingCommand;
import io.github.debeee.travelagency.command.application.exception.HotelNotFoundException;
import io.github.debeee.travelagency.command.application.port.out.AvailabilityRepository;
import io.github.debeee.travelagency.command.application.port.out.BookingRepository;
import io.github.debeee.travelagency.command.application.port.out.HotelRepository;
import io.github.debeee.travelagency.command.application.port.out.OutboxRepository;
import io.github.debeee.travelagency.command.domain.exception.OverbookingException;
import io.github.debeee.travelagency.command.domain.model.Booking;
import io.github.debeee.travelagency.command.domain.model.Hotel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.inOrder;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Long HOTEL_ID = 7L;
    private static final Long USER_ID = 100L;
    private static final long CAPACITY = 2;
    private static final LocalDate START = LocalDate.of(2027, 6, 1);
    private static final LocalDate END = LocalDate.of(2027, 6, 3);

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private OutboxRepository<Booking> bookingOutboxRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void shouldReturnSavedBookingIdWhenHotelExistsAndRoomsAreAvailable() {
        // given
        CreateBookingCommand command = new CreateBookingCommand(HOTEL_ID, USER_ID, START, END);
        Booking newBooking = new Booking(null, HOTEL_ID, USER_ID, START, END);
        Booking savedBooking = new Booking(42L, HOTEL_ID, USER_ID, START, END);
        given(hotelRepository.findHotel(HOTEL_ID)).willReturn(Optional.of(new Hotel(HOTEL_ID, CAPACITY)));
        given(bookingRepository.save(newBooking)).willReturn(savedBooking);

        // when
        Long bookingId = bookingService.createBooking(command);

        // then
        assertThat(bookingId).isEqualTo(42L);
    }

    @Test
    void shouldReserveAvailabilityThenSaveBookingThenWriteOutboxWhenBookingIsCreated() {
        // given
        CreateBookingCommand command = new CreateBookingCommand(HOTEL_ID, USER_ID, START, END);
        Booking newBooking = new Booking(null, HOTEL_ID, USER_ID, START, END);
        Booking savedBooking = new Booking(42L, HOTEL_ID, USER_ID, START, END);
        given(hotelRepository.findHotel(HOTEL_ID)).willReturn(Optional.of(new Hotel(HOTEL_ID, CAPACITY)));
        given(bookingRepository.save(newBooking)).willReturn(savedBooking);
        InOrder inOrder = inOrder(availabilityRepository, bookingRepository, bookingOutboxRepository);

        // when
        bookingService.createBooking(command);

        // then
        then(availabilityRepository).should(inOrder).reserveAvailability(HOTEL_ID, CAPACITY, START, END);
        then(bookingRepository).should(inOrder).save(newBooking);
        then(bookingOutboxRepository).should(inOrder).saveOutbox(savedBooking);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenStartIsAfterEnd() {
        // given
        CreateBookingCommand command = new CreateBookingCommand(HOTEL_ID, USER_ID, END, START);

        // when & then
        assertThatThrownBy(() -> bookingService.createBooking(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start date cannot be after end date");
        then(hotelRepository).shouldHaveNoInteractions();
        then(availabilityRepository).shouldHaveNoInteractions();
        then(bookingRepository).shouldHaveNoInteractions();
        then(bookingOutboxRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldThrowHotelNotFoundExceptionWhenHotelDoesNotExist() {
        // given
        CreateBookingCommand command = new CreateBookingCommand(HOTEL_ID, USER_ID, START, END);
        given(hotelRepository.findHotel(HOTEL_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookingService.createBooking(command))
                .isInstanceOf(HotelNotFoundException.class)
                .hasMessage("Hotel 7 not found");
        then(availabilityRepository).shouldHaveNoInteractions();
        then(bookingRepository).shouldHaveNoInteractions();
        then(bookingOutboxRepository).shouldHaveNoInteractions();
    }

    @Test
    void shouldNotSaveBookingNorOutboxWhenReservationIsRejected() {
        // given
        CreateBookingCommand command = new CreateBookingCommand(HOTEL_ID, USER_ID, START, END);
        given(hotelRepository.findHotel(HOTEL_ID)).willReturn(Optional.of(new Hotel(HOTEL_ID, CAPACITY)));
        willThrow(new OverbookingException("Hotel 7 overbooked on 2027-06-02. Capacity: 2, occupied: 2"))
                .given(availabilityRepository).reserveAvailability(HOTEL_ID, CAPACITY, START, END);

        // when & then
        assertThatThrownBy(() -> bookingService.createBooking(command))
                .isInstanceOf(OverbookingException.class);
        then(bookingRepository).shouldHaveNoInteractions();
        then(bookingOutboxRepository).shouldHaveNoInteractions();
    }
}
