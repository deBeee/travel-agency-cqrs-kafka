package io.github.debeee.travelagency.command.infrastructure.kafka.outbox;

public class OutboxSendException extends RuntimeException {
    public OutboxSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
