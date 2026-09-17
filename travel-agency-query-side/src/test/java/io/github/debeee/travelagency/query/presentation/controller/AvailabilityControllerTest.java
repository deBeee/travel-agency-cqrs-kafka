package io.github.debeee.travelagency.query.presentation.controller;

import io.github.debeee.travelagency.query.application.port.in.GetAvailabilityUseCase;
import io.github.debeee.travelagency.query.domain.model.Availability;
import io.github.debeee.travelagency.query.domain.model.AvailabilityStatus;
import io.github.debeee.travelagency.query.presentation.mapper.AvailabilityResponseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvailabilityController.class)
@Import(AvailabilityResponseMapper.class)
class AvailabilityControllerTest {

    private static final long HOTEL_ID = 7L;
    private static final LocalDate JUNE_1 = LocalDate.of(2027, 6, 1);
    private static final LocalDate JUNE_2 = LocalDate.of(2027, 6, 2);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAvailabilityUseCase getAvailabilityUseCase;

    @Test
    void shouldReturnOkWithAllProjectedDaysWhenNoRangeIsGiven() throws Exception {
        // given
        List<Availability> availabilities = List.of(
                new Availability(HOTEL_ID, JUNE_1, 9, 10, AvailabilityStatus.LAST_ROOMS),
                new Availability(HOTEL_ID, JUNE_2, 10, 10, AvailabilityStatus.SOLD_OUT));
        given(getAvailabilityUseCase.getForHotel(HOTEL_ID, null, null)).willReturn(availabilities);
        String expectedFirstDate = "2027-06-01";
        long expectedFirstFreeRooms = 1;
        String expectedFirstStatus = "LAST_ROOMS";
        String expectedSecondStatus = "SOLD_OUT";

        // when
        ResultActions result = mockMvc.perform(get("/api/availability/{hotelId}", HOTEL_ID));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(availabilities.size()))
                .andExpect(jsonPath("$[0].hotelId").value(HOTEL_ID))
                .andExpect(jsonPath("$[0].date").value(expectedFirstDate))
                .andExpect(jsonPath("$[0].occupied").value(9))
                .andExpect(jsonPath("$[0].capacity").value(10))
                .andExpect(jsonPath("$[0].freeRooms").value(expectedFirstFreeRooms))
                .andExpect(jsonPath("$[0].status").value(expectedFirstStatus))
                .andExpect(jsonPath("$[1].status").value(expectedSecondStatus));
    }

    @Test
    void shouldPassDateRangeToUseCaseWhenFromAndToAreGiven() throws Exception {
        // given
        given(getAvailabilityUseCase.getForHotel(HOTEL_ID, JUNE_1, JUNE_2)).willReturn(List.of());

        // when
        ResultActions result = mockMvc.perform(get("/api/availability/{hotelId}", HOTEL_ID)
                .param("from", "2027-06-01")
                .param("to", "2027-06-02"));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        then(getAvailabilityUseCase).should().getForHotel(HOTEL_ID, JUNE_1, JUNE_2);
    }

    @Test
    void shouldReturnBadRequestWhenOnlyFromIsGiven() throws Exception {
        // given
        String expectedMessage = "Parameters from and to must be provided together";

        // when
        ResultActions result = mockMvc.perform(get("/api/availability/{hotelId}", HOTEL_ID)
                .param("from", "2027-06-01"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
        then(getAvailabilityUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenFromIsAfterTo() throws Exception {
        // given
        String expectedMessage = "Parameter from cannot be after to";

        // when
        ResultActions result = mockMvc.perform(get("/api/availability/{hotelId}", HOTEL_ID)
                .param("from", "2027-06-02")
                .param("to", "2027-06-01"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
        then(getAvailabilityUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenDateHasInvalidFormat() throws Exception {
        // given
        String expectedMessage = "Invalid value for parameter 'from'";

        // when
        ResultActions result = mockMvc.perform(get("/api/availability/{hotelId}", HOTEL_ID)
                .param("from", "01-06-2027")
                .param("to", "2027-06-02"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
        then(getAvailabilityUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenHotelIdIsNotNumeric() throws Exception {
        // given
        String expectedMessage = "Invalid value for parameter 'hotelId'";

        // when
        ResultActions result = mockMvc.perform(get("/api/availability/{hotelId}", "abc"));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
        then(getAvailabilityUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnMethodNotAllowedWhenHttpMethodIsNotSupported() throws Exception {
        // given
        String expectedMessage = "Method 'POST' is not supported";

        // when
        ResultActions result = mockMvc.perform(post("/api/availability/{hotelId}", HOTEL_ID));

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
}
