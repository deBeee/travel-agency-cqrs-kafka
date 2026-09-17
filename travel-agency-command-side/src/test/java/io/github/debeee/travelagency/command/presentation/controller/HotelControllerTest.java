package io.github.debeee.travelagency.command.presentation.controller;

import io.github.debeee.travelagency.command.application.exception.HotelNotFoundException;
import io.github.debeee.travelagency.command.application.port.in.CreateHotelUseCase;
import io.github.debeee.travelagency.command.application.port.in.UpdateHotelCapacityUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HotelController.class)
class HotelControllerTest {

    private static final Long HOTEL_ID = 5L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "transactionalCreateHotelUseCase")
    private CreateHotelUseCase createHotelUseCase;

    @MockitoBean(name = "transactionalUpdateHotelCapacityUseCase")
    private UpdateHotelCapacityUseCase updateHotelCapacityUseCase;

    @Test
    void shouldReturnCreatedWithHotelIdWhenCapacityIsPositive() throws Exception {
        // given
        long capacity = 10;
        String requestBody = capacityJson(capacity);
        given(createHotelUseCase.createHotel(capacity)).willReturn(HOTEL_ID);

        // when
        ResultActions result = mockMvc.perform(post("/api/hotels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.hotelId").value(HOTEL_ID));
    }

    @Test
    void shouldReturnBadRequestWithFieldErrorWhenCreatedCapacityIsNotPositive() throws Exception {
        // given
        String requestBody = capacityJson(0);
        String expectedMessage = "Validation error";
        String expectedCapacityError = "Capacity must be positive";

        // when
        ResultActions result = mockMvc.perform(post("/api/hotels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage))
                .andExpect(jsonPath("$.validationErrors.capacity").value(expectedCapacityError));
        then(createHotelUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnOkWithNewCapacityWhenHotelExists() throws Exception {
        // given
        long newCapacity = 20;
        String requestBody = capacityJson(newCapacity);

        // when
        ResultActions result = mockMvc.perform(put("/api/hotels/{hotelId}", HOTEL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.hotelId").value(HOTEL_ID))
                .andExpect(jsonPath("$.capacity").value(newCapacity));
        then(updateHotelCapacityUseCase).should().updateCapacity(HOTEL_ID, newCapacity);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatedHotelDoesNotExist() throws Exception {
        // given
        long newCapacity = 20;
        String requestBody = capacityJson(newCapacity);
        String expectedMessage = "Hotel 5 not found";
        willThrow(new HotelNotFoundException(HOTEL_ID))
                .given(updateHotelCapacityUseCase).updateCapacity(HOTEL_ID, newCapacity);

        // when
        ResultActions result = mockMvc.perform(put("/api/hotels/{hotelId}", HOTEL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    void shouldReturnBadRequestWithFieldErrorWhenUpdatedCapacityIsNotPositive() throws Exception {
        // given
        String requestBody = capacityJson(-1);
        String expectedCapacityError = "Capacity must be positive";

        // when
        ResultActions result = mockMvc.perform(put("/api/hotels/{hotelId}", HOTEL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.capacity").value(expectedCapacityError));
        then(updateHotelCapacityUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenHotelIdIsNotNumeric() throws Exception {
        // given
        String requestBody = capacityJson(20);
        String expectedMessage = "Invalid value for parameter 'hotelId'";

        // when
        ResultActions result = mockMvc.perform(put("/api/hotels/{hotelId}", "abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
        then(updateHotelCapacityUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsNotValidJson() throws Exception {
        // given
        String requestBody = "{capacity:";
        String expectedMessage = "Malformed request body";

        // when
        ResultActions result = mockMvc.perform(post("/api/hotels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
        then(createHotelUseCase).shouldHaveNoInteractions();
    }

    private static String capacityJson(long capacity) {
        return """
                {"capacity": %d}
                """.formatted(capacity);
    }
}
