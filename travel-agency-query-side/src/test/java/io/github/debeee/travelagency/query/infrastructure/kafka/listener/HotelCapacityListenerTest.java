package io.github.debeee.travelagency.query.infrastructure.kafka.listener;

import io.github.debeee.travelagency.avro.HotelUpsertedAvro;
import io.github.debeee.travelagency.query.application.port.in.UpsertHotelCapacityUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class HotelCapacityListenerTest {

    @Mock
    private UpsertHotelCapacityUseCase upsertHotelCapacityUseCase;

    @InjectMocks
    private HotelCapacityListener listener;

    @Test
    void shouldForwardHotelIdAndCapacityWhenHotelEventArrives() {
        // given
        HotelUpsertedAvro event = HotelUpsertedAvro.newBuilder()
                .setHotelId(7L)
                .setCapacity(10L)
                .build();

        // when
        listener.onHotelUpserted(event);

        // then
        then(upsertHotelCapacityUseCase).should().upsert(7L, 10L);
    }
}
