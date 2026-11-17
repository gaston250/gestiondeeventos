package com.mdp.eventos.dao;

import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface IUsuarioDAO {
    Optional<Usuario> buscarPorEmail(String email) throws DatabaseException;

    List<Usuario> listar() throws DatabaseException;

    boolean actualizarEstado(long usuarioId, boolean activo, long administradorId) throws DatabaseException;

    void registrar(String nombre, String email, String passwordHash, String rolNombre)
        throws DatabaseException;
}
