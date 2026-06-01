package com.trinity.financial_api.repository;

import com.trinity.financial_api.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByClienteId(Long clienteId);

    boolean existsByNumeroCuenta(String numeroCuenta);
}
