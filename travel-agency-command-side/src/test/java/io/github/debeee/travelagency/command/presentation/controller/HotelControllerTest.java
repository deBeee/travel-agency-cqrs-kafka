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
        String requestBody = """
                {"capacity": 10}
                """;
        given(createHotelUseCase.createHotel(10)).willReturn(HOTEL_ID);

        // when
        ResultActions result = mockMvc.perform(post("/api/hotels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.hotelId").value(5));
    }

    @Test
    void shouldReturnBadRequestWithFieldErrorWhenCreatedCapacityIsNotPositive() throws Exception {
        // given
        String requestBody = """
                {"capacity": 0}
                """;

        // when
        ResultActions result = mockMvc.perform(post("/api/hotels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation error"))
                .andExpect(jsonPath("$.validationErrors.capacity").value("Capacity must be positive"));
        then(createHotelUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnOkWithNewCapacityWhenHotelExists() throws Exception {
        // given
        String requestBody = """
                {"capacity": 20}
                """;

        // when
        ResultActions result = mockMvc.perform(put("/api/hotels/{hotelId}", HOTEL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.hotelId").value(5))
                .andExpect(jsonPath("$.capacity").value(20));
        then(updateHotelCapacityUseCase).should().updateCapacity(HOTEL_ID, 20);
    }

    @Test
    void shouldReturnNotFoundWhenUpdatedHotelDoesNotExist() throws Exception {
        // given
        String requestBody = """
                {"capacity": 20}
                """;
        willThrow(new HotelNotFoundException(HOTEL_ID)).given(updateHotelCapacityUseCase).updateCapacity(HOTEL_ID, 20);

        // when
        ResultActions result = mockMvc.perform(put("/api/hotels/{hotelId}", HOTEL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Hotel 5 not found"));
    }

    @Test
    void shouldReturnBadRequestWithFieldErrorWhenUpdatedCapacityIsNotPositive() throws Exception {
        // given
        String requestBody = """
                {"capacity": -1}
                """;

        // when
        ResultActions result = mockMvc.perform(put("/api/hotels/{hotelId}", HOTEL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.capacity").value("Capacity must be positive"));
        then(updateHotelCapacityUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenHotelIdIsNotNumeric() throws Exception {
        // given
        String requestBody = """
                {"capacity": 20}
                """;

        // when
        ResultActions result = mockMvc.perform(put("/api/hotels/{hotelId}", "abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'hotelId'"));
        then(updateHotelCapacityUseCase).shouldHaveNoInteractions();
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsNotValidJson() throws Exception {
        // given
        String requestBody = "{capacity:";

        // when
        ResultActions result = mockMvc.perform(post("/api/hotels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
        then(createHotelUseCase).shouldHaveNoInteractions();
    }
}
