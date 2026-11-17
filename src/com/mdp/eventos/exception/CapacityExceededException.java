package com.mdp.eventos.exception;

public class CapacityExceededException extends BusinessValidationException {
    public CapacityExceededException(String message) {
        super(message);
    }
}
