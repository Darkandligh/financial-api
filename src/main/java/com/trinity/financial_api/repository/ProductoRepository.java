package com.trinity.financial_api.repository;

import com.trinity.financial_api.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByClienteId(Long clienteId);

    boolean existsByNumeroCuenta(String numeroCuenta);

    Optional<Producto> findByNumeroCuenta(String numeroCuenta);
}
