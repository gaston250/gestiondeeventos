package com.mdp.eventos.service;

import com.mdp.eventos.dao.IEventoCatalogoDAO;
import com.mdp.eventos.dao.ISalonDAO;
import com.mdp.eventos.dto.CotizacionSalonDTO;
import com.mdp.eventos.dto.FiltroBusquedaEventoDTO;
import com.mdp.eventos.dto.SalonDisponibilidadDTO;
import com.mdp.eventos.exception.BusinessValidationException;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.TipoEvento;
import com.mdp.eventos.model.ZonaMdp;
import com.mdp.eventos.model.Rol;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

public class EventoBusquedaService {
    private final SalonService salonService;
    private final CotizacionService cotizacionService;
    private final IEventoCatalogoDAO catalogoDAO;

    public EventoBusquedaService(ISalonDAO salonDAO) {
        this(salonDAO, new com.mdp.eventos.dao.impl.EventoCatalogoDAOImpl());
    }

    public EventoBusquedaService(
        ISalonDAO salonDAO,
        IEventoCatalogoDAO catalogoDAO
    ) {
        this.salonService = new SalonService(Objects.requireNonNull(salonDAO, "salonDAO"));
        this.cotizacionService = new CotizacionService();
        this.catalogoDAO = Objects.requireNonNull(catalogoDAO, "catalogoDAO");
    }

    public List<TipoEvento> listarTiposEvento() throws DatabaseException, BusinessValidationException {
        SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_CLIENTE);
        return catalogoDAO.listarTiposEvento();
    }

    public List<ZonaMdp> listarZonas() throws DatabaseException, BusinessValidationException {
        SeguridadSesion.exigirUsuarioActivo(Rol.ROLE_CLIENTE);
        return catalogoDAO.listarZonas();
    }

    public List<CotizacionSalonDTO> buscarYCotizar(
        int tipoEventoId,
        int invitados,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String zona
    ) throws BusinessValidationException, DatabaseException {
        return buscarYCotizar(new FiltroBusquedaEventoDTO(
            tipoEventoId, invitados, fecha, horaInicio, horaFin, zona
        ));
    }

    public List<CotizacionSalonDTO> buscarYCotizar(FiltroBusquedaEventoDTO filtro)
        throws BusinessValidationException, DatabaseException {
        List<SalonDisponibilidadDTO> salones = salonService.buscarDisponibles(filtro);
        if (salones.isEmpty()) {
            return List.of();
        }

        return cotizacionService.cotizar(salones, filtro);
    }
}
