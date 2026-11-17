package com.mdp.eventos.dao;

import com.mdp.eventos.dto.CatalogoAdminDTO;
import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.TipoCatalogoAdmin;

import java.util.List;

public interface ICatalogoAdminDAO {
    List<CatalogoAdminDTO> listar(TipoCatalogoAdmin tipo) throws DatabaseException;

    void crear(CatalogoAdminDTO catalogo) throws DatabaseException;

    boolean actualizar(CatalogoAdminDTO catalogo) throws DatabaseException;

    boolean cambiarEstado(TipoCatalogoAdmin tipo, long id, boolean activo) throws DatabaseException;
}
