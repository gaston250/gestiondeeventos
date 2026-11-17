package com.mdp.eventos.dao;

import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.TipoEvento;
import com.mdp.eventos.model.ZonaMdp;

import java.util.List;

public interface IEventoCatalogoDAO {
    List<TipoEvento> listarTiposEvento() throws DatabaseException;

    List<ZonaMdp> listarZonas() throws DatabaseException;
}
