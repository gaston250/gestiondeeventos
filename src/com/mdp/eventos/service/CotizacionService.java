package com.mdp.eventos.service;

import com.mdp.eventos.dto.CotizacionSalonDTO;
import com.mdp.eventos.dto.FiltroBusquedaEventoDTO;
import com.mdp.eventos.dto.SalonDisponibilidadDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class CotizacionService {
    public List<CotizacionSalonDTO> cotizar(
        List<SalonDisponibilidadDTO> salones,
        FiltroBusquedaEventoDTO filtro
    ) {
        BigDecimal horas = BigDecimal.valueOf(HorarioEvento.duracion(
                filtro.horaInicio(), filtro.horaFin()
            ).toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.CEILING);
        return salones.stream()
            .map(salon -> new CotizacionSalonDTO(
                salon.salonId(),
                salon.establecimiento(),
                salon.salon(),
                salon.zona(),
                salon.direccion(),
                salon.capacidadMaxima(),
                filtro.invitados(),
                filtro.horaInicio(),
                filtro.horaFin(),
                salon.tarifaBaseHora()
                    .multiply(horas)
                    .add(salon.costoPorPersona().multiply(BigDecimal.valueOf(filtro.invitados())))
                    .setScale(2, RoundingMode.HALF_UP),
                salon.tarifaBaseHora(),
                salon.costoPorPersona()
            ))
            .toList();
    }
}
