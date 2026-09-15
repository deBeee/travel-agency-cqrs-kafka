package io.github.debeee.travelagency.command.infrastructure.tx;

import io.github.debeee.travelagency.command.application.port.in.UpdateHotelCapacityUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Isolation.READ_COMMITTED;

@RequiredArgsConstructor
public class TransactionalUpdateHotelCapacityUseCase implements UpdateHotelCapacityUseCase {

    private final UpdateHotelCapacityUseCase delegate;

    @Override
    @Transactional(isolation = READ_COMMITTED)
    public void updateCapacity(Long hotelId, long capacity) {
        delegate.updateCapacity(hotelId, capacity);
    }
}
