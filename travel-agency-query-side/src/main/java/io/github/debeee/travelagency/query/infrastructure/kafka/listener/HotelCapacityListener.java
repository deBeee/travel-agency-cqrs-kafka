package io.github.debeee.travelagency.query.infrastructure.kafka.listener;

import io.github.debeee.travelagency.avro.HotelUpsertedAvro;
import io.github.debeee.travelagency.query.application.port.in.UpsertHotelCapacityUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotelCapacityListener {

    private final UpsertHotelCapacityUseCase upsertHotelCapacityUseCase;

    @KafkaListener(
            topics = "${app.topics.hotels}",
            groupId = "${app.hotels.consumer-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onHotelUpserted(HotelUpsertedAvro event) {
        upsertHotelCapacityUseCase.upsert(event.getHotelId(), event.getCapacity());
        log.debug("Hotel capacity upserted hotelId = {}, capacity = {}", event.getHotelId(), event.getCapacity());
    }
}
