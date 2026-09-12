package io.github.debeee.travelagency.command.infrastructure.kafka.outbox;

public class PayloadConversionException extends RuntimeException {
    public PayloadConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
