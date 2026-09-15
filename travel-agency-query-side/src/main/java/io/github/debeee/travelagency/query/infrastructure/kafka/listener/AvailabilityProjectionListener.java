package io.github.debeee.travelagency.query.infrastructure.kafka.listener;

import io.github.debeee.travelagency.avro.AvailabilityUpdatedAvro;
import io.github.debeee.travelagency.query.application.command.UpdateAvailabilityCommand;
import io.github.debeee.travelagency.query.application.port.in.UpdateAvailabilityUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AvailabilityProjectionListener {
    private final UpdateAvailabilityUseCase updateAvailabilityUseCase;

    @KafkaListener(
            topics = "${app.topics.availability}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAvailabilityUpdated(AvailabilityUpdatedAvro event) {
        UpdateAvailabilityCommand command = new UpdateAvailabilityCommand(
                event.getHotelId(),
                LocalDate.parse(event.getDate()),
                event.getOccupied()
        );
        
        updateAvailabilityUseCase.update(command);
        
        log.debug(
                "Projection upserted hotelId={}, date={}, occupied={}",
                event.getHotelId(),
                event.getDate(),
                event.getOccupied()
        );
    }
}
