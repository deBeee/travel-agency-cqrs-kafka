package io.github.debeee.travelagency.command.domain.exception;

public class OverbookingException extends RuntimeException {
    public OverbookingException(String message) {
        super(message);
    }
}
