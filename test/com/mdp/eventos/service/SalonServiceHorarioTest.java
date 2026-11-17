package com.mdp.eventos.service;

import com.mdp.eventos.dao.ISalonDAO;
import com.mdp.eventos.dto.SalonDisponibilidadDTO;
import com.mdp.eventos.dto.SalonProveedorDTO;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.ScheduleConflictException;
import com.mdp.eventos.model.Salon;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalonServiceHorarioTest {
    private final SalonService service = new SalonService(new ISalonDAO() {
        @Override
        public List<SalonDisponibilidadDTO> buscarDisponibles(
            int tipoEventoId, int invitados, LocalDate fecha, LocalTime inicio, LocalTime fin, String zona
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void crearParaProveedor(Salon salon, long proveedorId, List<Long> tipos, List<String> dias) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<SalonProveedorDTO> listarPorProveedor(long proveedorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> actualizarParaProveedor(
            Salon salon, long proveedorId, List<Long> tipos, List<String> dias
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<String> cambiarEstadoParaProveedor(long salonId, long proveedorId, boolean activo) {
            throw new UnsupportedOperationException();
        }
    });

    @Test
    void calculaHorarioQueCruzaMedianoche() {
        assertEquals(Duration.ofHours(4),
            HorarioEvento.duracion(LocalTime.of(22, 0), LocalTime.of(2, 0)));
        assertTrue(HorarioEvento.estaDentroDeJornada(
            LocalTime.of(20, 0), LocalTime.of(4, 0),
            LocalTime.of(22, 0), LocalTime.of(2, 0)
        ));
    }

    @Test
    void rechazaIntervaloDeCeroDuracion() {
        assertThrows(IllegalArgumentException.class,
            () -> HorarioEvento.duracion(LocalTime.NOON, LocalTime.NOON));
    }

    @Test
    void validaFechaHoraYRechazaFechaPasada() {
        assertThrows(BusinessValidationException.class,
            () -> service.validarHorario(LocalDate.now().minusDays(1), LocalTime.of(18, 0), LocalTime.of(20, 0)));
        assertThrows(ScheduleConflictException.class,
            () -> service.validarHorario(LocalDate.now(), LocalTime.NOON, LocalTime.NOON));
    }
}
