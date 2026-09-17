package io.github.debeee.travelagency.query.infrastructure.kafka.listener;

import io.github.debeee.travelagency.avro.AvailabilityUpdatedAvro;
import io.github.debeee.travelagency.query.application.command.UpdateAvailabilityCommand;
import io.github.debeee.travelagency.query.application.port.in.UpdateAvailabilityUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AvailabilityProjectionListenerTest {

    @Mock
    private UpdateAvailabilityUseCase updateAvailabilityUseCase;

    @InjectMocks
    private AvailabilityProjectionListener listener;

    @Test
    void shouldMapEventToCommandWithParsedDateWhenAvailabilityEventArrives() {
        // given
        AvailabilityUpdatedAvro event = AvailabilityUpdatedAvro.newBuilder()
                .setHotelId(7L)
                .setDate("2027-06-01")
                .setOccupied(3L)
                .build();
        UpdateAvailabilityCommand expectedCommand = new UpdateAvailabilityCommand(7L, LocalDate.of(2027, 6, 1), 3L);

        // when
        listener.onAvailabilityUpdated(event);

        // then
        then(updateAvailabilityUseCase).should().update(expectedCommand);
    }
}
