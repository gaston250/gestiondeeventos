package com.mdp.eventos.exception;

public class ScheduleConflictException extends BusinessValidationException {
    public ScheduleConflictException(String message) {
        super(message);
    }
}
