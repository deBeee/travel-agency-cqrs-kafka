package io.github.debeee.travelagency.command.infrastructure.tx;

import io.github.debeee.travelagency.command.application.command.CreateBookingCommand;
import io.github.debeee.travelagency.command.application.port.in.CreateBookingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class TransactionalCreateBookingUseCase implements CreateBookingUseCase {

    private final CreateBookingUseCase createBookingUseCase;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long createBooking(CreateBookingCommand command) {
        return createBookingUseCase.createBooking(command);
    }
}