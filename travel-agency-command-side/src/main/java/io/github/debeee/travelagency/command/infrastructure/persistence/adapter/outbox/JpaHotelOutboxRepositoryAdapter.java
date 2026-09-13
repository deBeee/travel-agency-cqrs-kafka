package io.github.debeee.travelagency.command.infrastructure.persistence.adapter.outbox;

import io.github.debeee.travelagency.command.application.event.HotelUpsertedPayload;
import io.github.debeee.travelagency.command.application.port.out.OutboxRepository;
import io.github.debeee.travelagency.command.domain.model.Hotel;
import io.github.debeee.travelagency.command.infrastructure.kafka.properties.HotelsTopicProperties;
import io.github.debeee.travelagency.command.infrastructure.persistence.entity.OutboxEntity;
import io.github.debeee.travelagency.command.infrastructure.persistence.mapper.OutboxMapper;
import io.github.debeee.travelagency.command.infrastructure.persistence.repository.JpaOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaHotelOutboxRepositoryAdapter implements OutboxRepository<Hotel> {

    private final JpaOutboxRepository jpaOutboxRepository;
    private final HotelsTopicProperties hotelsTopicProperties;
    private final OutboxMapper outboxMapper;

    private static final String HOTEL_UPSERTED_EVENT_TYPE = "HotelUpserted";

    @Override
    public void saveOutbox(Hotel hotel) {
        var payload = new HotelUpsertedPayload(hotel.getId(), hotel.getCapacity());

        OutboxEntity outbox = outboxMapper.toOutboxEntity(
                payload,
                hotelsTopicProperties.name(),
                hotel.getId().toString(),
                HOTEL_UPSERTED_EVENT_TYPE
        );

        jpaOutboxRepository.save(outbox);
    }
}
