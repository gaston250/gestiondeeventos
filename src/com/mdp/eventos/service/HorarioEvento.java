package com.mdp.eventos.service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

public final class HorarioEvento {
    private static final long MINUTOS_DIA = 24 * 60;

    private HorarioEvento() {
    }

    public static Duration duracion(LocalTime inicio, LocalTime fin) {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fin, "fin");
        long minutos = Duration.between(inicio, fin).toMinutes();
        if (minutos < 0) minutos += MINUTOS_DIA;
        if (minutos == 0) {
            throw new IllegalArgumentException("La hora de inicio y fin no pueden ser iguales.");
        }
        return Duration.ofMinutes(minutos);
    }

    public static boolean estaDentroDeJornada(LocalTime apertura, LocalTime cierre, LocalTime inicio, LocalTime fin) {
        Objects.requireNonNull(apertura, "apertura");
        Objects.requireNonNull(cierre, "cierre");
        long duracionEvento = duracion(inicio, fin).toMinutes();
        if (apertura.equals(cierre)) return duracionEvento <= MINUTOS_DIA;
        long duracionJornada = Math.floorMod(
            Duration.between(apertura, cierre).toMinutes(),
            MINUTOS_DIA
        );

        long desplazamientoInicio = Math.floorMod(
            Duration.between(apertura, inicio).toMinutes(),
            MINUTOS_DIA
        );
        return desplazamientoInicio < duracionJornada
            && desplazamientoInicio + duracionEvento <= duracionJornada;
    }

    public static boolean cruzaMedianoche(LocalTime inicio, LocalTime fin) {
        return Objects.requireNonNull(fin, "fin").isBefore(Objects.requireNonNull(inicio, "inicio"));
    }
}
