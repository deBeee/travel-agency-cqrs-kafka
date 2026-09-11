package io.github.debeee.travelagency.command.application.port.out;

public interface OutboxRepository<T> {
    void saveOutbox(T domain);
}
