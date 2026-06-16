package com.trinity.financial_api.repository;

import com.trinity.financial_api.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {

    @Query("SELECT t FROM Transaccion t " +
           "LEFT JOIN FETCH t.cuentaOrigen " +
           "LEFT JOIN FETCH t.cuentaDestino " +
           "WHERE t.cuentaOrigen.numeroCuenta = :numeroCuenta " +
           "ORDER BY t.fecha DESC")
    List<Transaccion> findByCuentaNumero(@Param("numeroCuenta") String numeroCuenta);
}
