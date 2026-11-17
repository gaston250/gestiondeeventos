package com.mdp.eventos.service;

import com.mdp.eventos.config.SesionActual;
import com.mdp.eventos.dao.IReservaDAO;
import com.mdp.eventos.dao.IServicioAdicionalDAO;
import com.mdp.eventos.dao.impl.ServicioAdicionalDAOImpl;
import com.mdp.eventos.dto.CrearReservaDTO;
import com.mdp.eventos.dto.FiltroReservasDTO;
import com.mdp.eventos.dto.ResumenReservaDTO;
import com.mdp.eventos.dto.ReservaProveedorDTO;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.CapacityExceededException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.exception.ScheduleConflictException;
import com.mdp.eventos.model.EstadoReserva;
import com.mdp.eventos.model.Rol;
import com.mdp.eventos.model.Usuario;
import com.mdp.eventos.model.Reserva;
import com.mdp.eventos.model.DetalleReservaServicio;
import com.mdp.eventos.model.ServicioAdicional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ReservaService {
    private final IReservaDAO reservaDAO;
    private final IServicioAdicionalDAO servicioAdicionalDAO;

    public ReservaService(IReservaDAO reservaDAO) {
        this(reservaDAO, new ServicioAdicionalDAOImpl());
    }

    public ReservaService(IReservaDAO reservaDAO, IServicioAdicionalDAO servicioAdicionalDAO) {
        this.reservaDAO = Objects.requireNonNull(reservaDAO, "reservaDAO");
        this.servicioAdicionalDAO = Objects.requireNonNull(servicioAdicionalDAO, "servicioAdicionalDAO");
    }

    public Reserva crear(CrearReservaDTO solicitud)
        throws BusinessValidationException, DatabaseException, ScheduleConflictException {
        Usuario cliente = SesionActual.getUsuario()
            .filter(usuario -> usuario.getRol() != null
                && Rol.ROLE_CLIENTE.equals(usuario.getRol().getNombre()))
            .orElseThrow(() -> new BusinessValidationException("Debe iniciar sesión como cliente para reservar."));
        if (solicitud == null) {
            throw new BusinessValidationException("Faltan los datos de la reserva.");
        }
        if (solicitud.fechaEvento() == null || solicitud.fechaEvento().isBefore(LocalDate.now())) {
            throw new BusinessValidationException("La fecha de reserva debe ser hoy o futura.");
        }
        if (solicitud.horaInicio() == null || solicitud.horaFin() == null
            || solicitud.horaFin().equals(solicitud.horaInicio())) {
            throw new BusinessValidationException("Ingrese horas válidas y distintas para la reserva.");
        }
        if (solicitud.cantidadInvitados() <= 0) {
            throw new BusinessValidationException("La cantidad de invitados debe ser mayor que cero.");
        }

        IReservaDAO.CondicionesSalon salon = reservaDAO.buscarCondicionesSalon(
            solicitud.salonId(),
            solicitud.tipoEventoId(),
            solicitud.fechaEvento(),
            solicitud.horaInicio(),
            solicitud.horaFin()
        );
        if (salon == null) {
            throw new BusinessValidationException(
                "El salón no existe, no admite ese tipo de evento o no está disponible ese día."
            );
        }
        if (solicitud.cantidadInvitados() < salon.capacidadMinima()
            || solicitud.cantidadInvitados() > salon.capacidadMaxima()) {
            throw new CapacityExceededException(
                "La capacidad permitida para el salón es de " + salon.capacidadMinima()
                    + " a " + salon.capacidadMaxima() + " invitados."
            );
        }
        if (!HorarioEvento.estaDentroDeJornada(
            salon.horaApertura(), salon.horaCierre(), solicitud.horaInicio(), solicitud.horaFin()
        )) {
            throw new BusinessValidationException("El horario está fuera del horario operativo del salón.");
        }

        BigDecimal horas = BigDecimal.valueOf(HorarioEvento.duracion(
                solicitud.horaInicio(), solicitud.horaFin()
            ).toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.CEILING);
        BigDecimal monto = salon.tarifaBaseHora().multiply(horas)
            .add(salon.costoPorPersona().multiply(BigDecimal.valueOf(solicitud.cantidadInvitados())));

        List<DetalleReservaServicio> detalles = new ArrayList<>();
        if (!solicitud.serviciosAdicionalesIds().isEmpty()) {
            Map<Long, ServicioAdicional> disponibles = new HashMap<>();
            for (ServicioAdicional servicio : servicioAdicionalDAO.listarPorSalon(solicitud.salonId())) {
                disponibles.put(servicio.getId(), servicio);
            }
            Set<Long> únicos = new HashSet<>();
            for (Long servicioId : solicitud.serviciosAdicionalesIds()) {
                if (servicioId == null || servicioId <= 0 || !únicos.add(servicioId)) {
                    throw new BusinessValidationException("La selección de servicios adicionales no es válida.");
                }
                ServicioAdicional servicio = disponibles.get(servicioId);
                if (servicio == null) {
                    throw new BusinessValidationException(
                        "Un servicio seleccionado ya no está disponible para este salón."
                    );
                }
                int cantidad = servicio.isCobroPorInvitado() ? solicitud.cantidadInvitados() : 1;
                DetalleReservaServicio detalle = new DetalleReservaServicio();
                detalle.setServicioId(servicio.getId());
                detalle.setCantidad(cantidad);
                detalle.setPrecioUnitarioPactado(servicio.getPrecio());
                detalles.add(detalle);
                monto = monto.add(servicio.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
            }
        }
        monto = monto.setScale(2, RoundingMode.HALF_UP);
        if (solicitud.montoTotalEsperado() != null
            && monto.compareTo(solicitud.montoTotalEsperado()) != 0) {
            throw new BusinessValidationException(
                "El precio cambió desde que se mostró la cotización. Vuelva a buscar y confirme el nuevo total."
            );
        }

        Reserva reserva = new Reserva();
        reserva.setCodigoReserva(UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
        reserva.setClienteId(cliente.getId());
        reserva.setSalonId(solicitud.salonId());
        reserva.setTipoEventoId(solicitud.tipoEventoId());
        reserva.setFechaEvento(solicitud.fechaEvento());
        reserva.setHoraInicio(solicitud.horaInicio());
        reserva.setHoraFin(solicitud.horaFin());
        reserva.setCantidadInvitados(solicitud.cantidadInvitados());
        reserva.setMontoTotal(monto);
        reserva.setEstado(EstadoReserva.PENDIENTE);
        reserva.setServiciosAdicionales(detalles);
        return reservaDAO.crear(reserva);
    }

    public List<ResumenReservaDTO> listarDelCliente(FiltroReservasDTO filtro)
        throws BusinessValidationException, DatabaseException {
        Usuario cliente = obtenerClienteActual();
        if (filtro == null) throw new BusinessValidationException("Los filtros de reservas son obligatorios.");
        if (filtro.desde() != null && filtro.hasta() != null && filtro.desde().isAfter(filtro.hasta())) {
            throw new BusinessValidationException("La fecha Desde no puede ser posterior a la fecha Hasta.");
        }
        EstadoReserva estado = null;
        if (filtro.estado() != null && !filtro.estado().isBlank() && !"Todos".equalsIgnoreCase(filtro.estado())) {
            try {
                estado = EstadoReserva.valueOf(filtro.estado().trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new BusinessValidationException("El estado de reserva seleccionado no es válido.", exception);
            }
        }
        return reservaDAO.listarPorCliente(cliente.getId(), estado, filtro.desde(), filtro.hasta());
    }

    public boolean cancelarDelCliente(long reservaId) throws BusinessValidationException, DatabaseException {
        if (reservaId <= 0) throw new BusinessValidationException("Seleccione una reserva válida.");
        Usuario cliente = obtenerClienteActual();
        return reservaDAO.cancelarAntesDelInicio(reservaId, cliente.getId());
    }

    public List<ReservaProveedorDTO> listarDelProveedorActual()
        throws BusinessValidationException, DatabaseException {
        return reservaDAO.listarPorProveedor(obtenerProveedorActual().getId());
    }

    public void actualizarEstadoComoProveedor(long reservaId, EstadoReserva nuevoEstado)
        throws BusinessValidationException, DatabaseException {
        actualizarEstadoComoProveedor(reservaId, nuevoEstado, null);
    }

    public void actualizarEstadoComoProveedor(long reservaId, EstadoReserva nuevoEstado, String motivo)
        throws BusinessValidationException, DatabaseException {
        Usuario proveedor = obtenerProveedorActual();
        if (reservaId <= 0) throw new BusinessValidationException("Seleccione una solicitud válida.");
        if (nuevoEstado != EstadoReserva.CONFIRMADA && nuevoEstado != EstadoReserva.RECHAZADA) {
            throw new BusinessValidationException("El proveedor solo puede confirmar o rechazar solicitudes pendientes.");
        }
        String motivoNormalizado = motivo == null ? "" : motivo.trim();
        if (nuevoEstado == EstadoReserva.RECHAZADA
            && (motivoNormalizado.isEmpty() || motivoNormalizado.length() > 500)) {
            throw new BusinessValidationException("El motivo del rechazo es obligatorio y admite hasta 500 caracteres.");
        }
        if (!reservaDAO.actualizarEstadoPendiente(
            reservaId, proveedor.getId(), nuevoEstado,
            nuevoEstado == EstadoReserva.RECHAZADA ? motivoNormalizado : null
        )) {
            throw new BusinessValidationException(
                "La solicitud ya fue atendida, no está pendiente, no pertenece a tus establecimientos "
                    + "o el evento ya comenzó."
            );
        }
    }

    public void cancelarComoProveedor(long reservaId, String motivo)
        throws BusinessValidationException, DatabaseException {
        Usuario proveedor = obtenerProveedorActual();
        String detalle = motivo == null ? "" : motivo.trim();
        if (reservaId <= 0 || detalle.isEmpty() || detalle.length() > 500) {
            throw new BusinessValidationException("El motivo de cancelación es obligatorio (máximo 500 caracteres).");
        }
        if (!reservaDAO.cancelarPorProveedor(reservaId, proveedor.getId(), proveedor.getId(), detalle)) {
            throw new BusinessValidationException(
                "Solo puedes cancelar una reserva pendiente o confirmada de tus establecimientos antes del inicio."
            );
        }
    }

    private Usuario obtenerClienteActual() throws BusinessValidationException, DatabaseException {
        return SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_CLIENTE);
    }

    private Usuario obtenerProveedorActual() throws BusinessValidationException, DatabaseException {
        return SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_PROVEEDOR);
    }
}
