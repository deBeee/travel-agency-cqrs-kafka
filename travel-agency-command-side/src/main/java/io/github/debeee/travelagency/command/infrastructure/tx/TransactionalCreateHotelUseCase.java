package io.github.debeee.travelagency.command.infrastructure.tx;

import io.github.debeee.travelagency.command.application.port.in.CreateHotelUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Isolation.READ_COMMITTED;

@RequiredArgsConstructor
public class TransactionalCreateHotelUseCase implements CreateHotelUseCase {

    private final CreateHotelUseCase delegate;

    @Override
    @Transactional(isolation = READ_COMMITTED)
    public Long createHotel(long capacity) {
        return delegate.createHotel(capacity);
    }
}
