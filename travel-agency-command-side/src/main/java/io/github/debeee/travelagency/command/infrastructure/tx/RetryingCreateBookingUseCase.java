package io.github.debeee.travelagency.command.infrastructure.tx;

import io.github.debeee.travelagency.command.application.command.CreateBookingCommand;
import io.github.debeee.travelagency.command.application.port.in.CreateBookingUseCase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public class RetryingCreateBookingUseCase implements CreateBookingUseCase {

    private final CreateBookingUseCase delegate;

    public RetryingCreateBookingUseCase(CreateBookingUseCase delegate) {
        this.delegate = delegate;
    }

    @Override
    @Retryable(
            retryFor = DataIntegrityViolationException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    public Long createBooking(CreateBookingCommand command) {
        return delegate.createBooking(command);
    }
}
