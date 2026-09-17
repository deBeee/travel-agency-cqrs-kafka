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
        Long expectedBookingId = 42L;
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, START, END)))
                .willReturn(expectedBookingId);

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(expectedBookingId));
    }

    @Test
    void shouldReturnBadRequestWithFieldErrorsWhenRequiredFieldsAreMissing() throws Exception {
        // given
        String requestBody = "{}";
        String expectedMessage = "Validation error";
        String expectedHotelIdError = "Hotel id is required";
        String expectedUserIdError = "User id is required";
        String expectedStartError = "Start date is required";
        String expectedEndError = "End date is required";

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage))
                .andExpect(jsonPath("$.validationErrors.hotelId").value(expectedHotelIdError))
                .andExpect(jsonPath("$.validationErrors.userId").value(expectedUserIdError))
                .andExpect(jsonPath("$.validationErrors.start").value(expectedStartError))
                .andExpect(jsonPath("$.validationErrors.end").value(expectedEndError));
        then(createBookingUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenStartDateIsInThePast() throws Exception {
        // given
        LocalDate yesterday = LocalDate.now().minusDays(1);
        String requestBody = bookingJson(HOTEL_ID, USER_ID, yesterday, END);
        String expectedStartError = "Start date must not be in the past";

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.start").value(expectedStartError));
        then(createBookingUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsNotValidJson() throws Exception {
        // given
        String requestBody = "this is not json";
        String expectedMessage = "Malformed request body";

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
        then(createBookingUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenStartIsAfterEnd() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, END, START);
        String expectedMessage = "Start date cannot be after end date";
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, END, START)))
                .willThrow(new IllegalArgumentException(expectedMessage));

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void shouldReturnNotFoundWhenHotelDoesNotExist() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);
        String expectedMessage = "Hotel 7 not found";
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, START, END)))
                .willThrow(new HotelNotFoundException(HOTEL_ID));

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void shouldReturnConflictWhenHotelIsOverbooked() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);
        String expectedMessage = "Hotel 7 overbooked on " + START + ". Capacity: 2, occupied: 2";
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, START, END)))
                .willThrow(new OverbookingException(expectedMessage));

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void shouldReturnConflictWhenConcurrentBookingIsDetected() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);
        String expectedMessage = "Concurrent booking detected. Please retry.";
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, START, END)))
                .willThrow(new DataIntegrityViolationException("Duplicate entry for key 'daily_availabilities.PRIMARY'"));

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void shouldReturnInternalServerErrorWithGenericMessageWhenUnexpectedErrorOccurs() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);
        String expectedMessage = "Unexpected error occurred";
        given(createBookingUseCase.createBooking(new CreateBookingCommand(HOTEL_ID, USER_ID, START, END)))
                .willThrow(new IllegalStateException("Connection pool exhausted"));

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void shouldReturnUnsupportedMediaTypeWhenContentTypeIsNotJson() throws Exception {
        // given
        String requestBody = bookingJson(HOTEL_ID, USER_ID, START, END);
        String expectedMessage = "Unsupported media type";

        // when
        ResultActions result = mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.TEXT_PLAIN)
                .content(requestBody));

        // then
        result.andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value(expectedMessage));
        then(createBookingUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnMethodNotAllowedWhenHttpMethodIsNotSupported() throws Exception {
        // given
        String expectedMessage = "Method 'GET' is not supported";

        // when
        ResultActions result = mockMvc.perform(get("/api/bookings"));

        // then
        result.andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void shouldReturnNotFoundWhenResourceDoesNotExist() throws Exception {
        // given
        String expectedMessage = "Resource not found";

        // when
        ResultActions result = mockMvc.perform(get("/api/unknown"));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private static String bookingJson(Long hotelId, Long userId, LocalDate start, LocalDate end) {
        return """
                {"hotelId": %d, "userId": %d, "start": "%s", "end": "%s"}
                """.formatted(hotelId, userId, start, end);
    }
}
