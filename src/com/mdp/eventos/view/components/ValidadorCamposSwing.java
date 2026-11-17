package com.mdp.eventos.view.components;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public final class ValidadorCamposSwing {
    private ValidadorCamposSwing() {
    }

    public static LocalDate parsearFecha(String valor) throws DateTimeParseException {
        return LocalDate.parse(valor);
    }

    public static LocalTime parsearHora(String valor) throws DateTimeParseException {
        return LocalTime.parse(valor);
    }
}
