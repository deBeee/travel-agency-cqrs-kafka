package io.github.debeee.travelagency.command.presentation.controller;

import io.github.debeee.travelagency.command.application.command.CreateBookingCommand;
import io.github.debeee.travelagency.command.application.exception.HotelNotFoundException;
import io.github.debeee.travelagency.command.application.port.in.CreateBookingUseCase;
import io.github.debeee.travelagency.command.domain.exception.OverbookingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final Long HOTEL_ID = 7L;
    private static final Long USER_ID = 100L;
    private static final LocalDate START = LocalDate.now().plusDays(30);
    private static final LocalDate END = LocalDate.now().plusDays(32);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "retryingCreateBookingUseCase")
    private CreateBookingUseCase createBookingUseCase;

    @Test
    void shouldReturnCreatedWithBookingIdWhenRequestIsValid() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, START, END)))
                .willReturn(42L);

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(42));
    }

    @Test
    void shouldReturnBadRequestWithFieldErrorsWhenRequiredFieldsAreMissing() throws Exception {
        // given
        String requestBody = "{}";

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.validationErrors.hotelId").value("Hotel id is required"))
                .andExpect(jsonPath("$.validationErrors.userId").value("User id is required"))
                .andExpect(jsonPath("$.validationErrors.start").value("Start date is required"))
                .andExpect(jsonPath("$.validationErrors.end").value("End date is required"));
        then(createBookingUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenStartDateIsInThePast() throws Exception {
        // given
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String requestBody = bookingJson(HOTEL_ID, USER_ID, yesterday, END);

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.start").value("Start date must not be in the past"));
        then(createBookingUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsNotValidJson() throws Exception {
        // given
        String requestBody = "this is not json";

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
        then(createBookingUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenStartIsAfterEnd() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, END, START);
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, END, START)))
                .willThrow(new IllegalArgumentException("Start date cannot be after end date"));

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start date cannot be after end date"));
    }

    @Test
    void shouldReturnNotFoundWhenHotelDoesNotExist() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, START, END)))
                .willThrow(new HotelNotFoundException(HOTEL_ID));

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Hotel 7 not found"));
    }

    @Test
    void shouldReturnConflictWhenHotelIsOverbooked() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);
        String overbookingMessage = "Hotel 7 overbooked on " + START + ". Capacity: 2, occupied: 2";
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, START, END)))
                .willThrow(new OverbookingException(overbookingMessage));

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(overbookingMessage));
    }

    @Test
    void shouldReturnConflictWhenConcurrentBookingIsDetected() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, START, END)))
                .willThrow(new DataIntegrityViolationException("Duplicate entry for key 'daily_availabilities.PRIMARY'"));

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Concurrent booking detected. Please retry."));
    }

    @Test
    void shouldReturnInternalServerErrorWithGenericMessageWhenUnexpectedErrorOccurs() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, START, END)))
                .willThrow(new IllegalStateException("Connection pool exhausted"));

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error occurred"));
    }

    @Test
    void shouldReturnUnsupportedMediaTypeWhenContentTypeIsNotJson() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.TEXT_PLAIN)
                .content(requestBody));

        // then
        result.andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Unsupported media type"));
        then(createBookingUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnMethodNotAllowedWhenHttpMethodIsNotSupported() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/bookings"));

        // then
        result.andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Method 'GET' is not supported"));
    }

    @Test
    void shouldReturnNotFoundWhenResourceDoesNotExist() throws Exception {
        // when
        ResultActions result = mockMvc.perform(get("/api/unknown"));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    private static String bookingJson(Long hotelId, Long userId, LocalDate start, LocalDate end) {
        return """
                {"hotelId": %d, "userId": %d, "start": "%s", "end": "%s"}
                """.formatted(hotelId, userId, start, end);
    }
}
