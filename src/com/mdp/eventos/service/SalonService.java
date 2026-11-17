package com.mdp.eventos.service;

import com.mdp.eventos.dao.ISalonDAO;
import com.mdp.eventos.config.SesionActual;
import com.mdp.eventos.dto.FiltroBusquedaEventoDTO;
import com.mdp.eventos.dto.SalonDisponibilidadDTO;
import com.mdp.eventos.dto.SalonProveedorDTO;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.exception.ScheduleConflictException;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.Salon;
import com.mdp.eventos.model.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SalonService {
    private static final Set<String> DIAS_SEMANA = Set.of(
        "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"
    );
    private final ISalonDAO salonDAO;

    public SalonService(ISalonDAO salonDAO) {
        this.salonDAO = Objects.requireNonNull(salonDAO, "salonDAO");
    }

    public List<SalonDisponibilidadDTO> buscarDisponibles(FiltroBusquedaEventoDTO filtro)
        throws BusinessValidationException, DatabaseException {
        SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_CLIENTE);
        validarFiltro(filtro);
        return salonDAO.buscarDisponibles(
            filtro.tipoEventoId(),
            filtro.invitados(),
            filtro.fecha(),
            filtro.horaInicio(),
            filtro.horaFin(),
            filtro.zona()
        );
    }

    public void crearParaProveedorActual(
        Salon salon,
        List<Long> tiposEvento,
        List<String> diasDisponibles
    ) throws BusinessValidationException, DatabaseException {
        Usuario proveedor = SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
        List<Long> tipos = validarDatosSalon(salon, tiposEvento, diasDisponibles);
        salonDAO.crearParaProveedor(salon, proveedor.getId(), tipos, normalizarDias(diasDisponibles));
    }

    public List<SalonProveedorDTO> listarDelProveedorActual()
        throws BusinessValidationException, DatabaseException {
        Usuario proveedor = SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
        return salonDAO.listarPorProveedor(proveedor.getId());
    }

    public void actualizarDelProveedorActual(
        Salon salon, List<Long> tiposEvento, List<String> diasDisponibles
    ) throws BusinessValidationException, DatabaseException {
        Usuario proveedor = SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
        List<Long> tipos = validarDatosSalon(salon, tiposEvento, diasDisponibles);
        if (salon.getId() <= 0) throw new BusinessValidationException("Seleccione un salón válido.");
        List<String> conflictos = salonDAO.actualizarParaProveedor(
            salon, proveedor.getId(), tipos, normalizarDias(diasDisponibles)
        );
        if (!conflictos.isEmpty()) {
            throw new BusinessValidationException("No se guardaron los cambios porque afectarían estas reservas:\n"
                + String.join("\n", conflictos));
        }
    }

    public void cambiarEstadoDelProveedorActual(long salonId, boolean activo)
        throws BusinessValidationException, DatabaseException {
        Usuario proveedor = SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
        if (salonId <= 0) throw new BusinessValidationException("Seleccione un salón válido.");
        List<String> conflictos = salonDAO.cambiarEstadoParaProveedor(salonId, proveedor.getId(), activo);
        if (!conflictos.isEmpty()) {
            throw new BusinessValidationException("No se puede desactivar el salón porque tiene reservas futuras:\n"
                + String.join("\n", conflictos));
        }
    }

    private List<Long> validarDatosSalon(Salon salon, List<Long> tiposEvento, List<String> diasDisponibles)
        throws BusinessValidationException {
        if (salon == null) throw new BusinessValidationException("Ingrese los datos del salón.");
        String nombre = salon.getNombreSalon() == null ? "" : salon.getNombreSalon().trim();
        if (salon.getEstablecimientoId() <= 0) {
            throw new BusinessValidationException("Seleccione un establecimiento propio activo.");
        }
        if (nombre.isEmpty() || nombre.length() > 100) {
            throw new BusinessValidationException("El nombre del salón es obligatorio y admite hasta 100 caracteres.");
        }
        if (salon.getCapacidadMinima() <= 0
            || salon.getCapacidadMaxima() < salon.getCapacidadMinima()) {
            throw new BusinessValidationException("Revise la capacidad mínima y máxima de invitados.");
        }
        if (salon.getHoraApertura() == null || salon.getHoraCierre() == null
            || salon.getHoraApertura().equals(salon.getHoraCierre())) {
            throw new BusinessValidationException("Ingrese horarios distintos de apertura y cierre.");
        }
        validarTarifa(salon.getTarifaBaseHora(), "La tarifa por hora");
        validarTarifa(salon.getCostoPorPersona(), "El costo por persona");
        List<Long> tipos = tiposEvento == null ? List.of() : tiposEvento.stream().distinct().toList();
        if (tipos.isEmpty() || tipos.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessValidationException("Seleccione al menos un tipo de evento.");
        }
        List<String> dias = diasDisponibles == null ? List.of() : diasDisponibles.stream().distinct().toList();
        if (dias.isEmpty() || dias.stream().anyMatch(dia -> dia == null || !DIAS_SEMANA.contains(dia))) {
            throw new BusinessValidationException("Seleccione al menos un día semanal válido.");
        }
        salon.setNombreSalon(nombre);
        return tipos;
    }

    private List<String> normalizarDias(List<String> dias) {
        return dias == null ? List.of() : dias.stream().distinct().toList();
    }

    private void validarTarifa(BigDecimal tarifa, String campo) throws BusinessValidationException {
        if (tarifa == null || tarifa.signum() <= 0 || tarifa.scale() > 2 || tarifa.precision() > 12) {
            throw new BusinessValidationException(campo + " debe ser mayor que cero y admitir hasta 2 decimales.");
        }
    }

    public void validarHorario(LocalDate fecha, java.time.LocalTime inicio, java.time.LocalTime fin)
        throws BusinessValidationException {
        if (fecha == null || fecha.isBefore(LocalDate.now())) {
            throw new BusinessValidationException("La fecha del evento no puede ser anterior a hoy.");
        }
        if (inicio == null || fin == null) {
            throw new ScheduleConflictException("Ingrese la hora de inicio y fin.");
        }
        if (inicio.equals(fin)) {
            throw new ScheduleConflictException("La hora de inicio y fin no pueden ser iguales.");
        }
        HorarioEvento.duracion(inicio, fin);
    }

    private void validarFiltro(FiltroBusquedaEventoDTO filtro) throws BusinessValidationException {
        if (filtro == null || filtro.tipoEventoId() <= 0) {
            throw new BusinessValidationException("Seleccione un tipo de evento.");
        }
        if (filtro.invitados() <= 0) {
            throw new BusinessValidationException("La cantidad de invitados debe ser mayor que cero.");
        }
        validarHorario(filtro.fecha(), filtro.horaInicio(), filtro.horaFin());
    }
}
