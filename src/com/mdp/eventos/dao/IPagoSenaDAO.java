package com.mdp.eventos.dao;

import com.mdp.eventos.exception.DatabaseException;
import com.mdp.eventos.model.PagoSena;

public interface IPagoSenaDAO {
    PagoSena registrar(PagoSena pago) throws DatabaseException;
}
