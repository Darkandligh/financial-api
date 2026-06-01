package com.trinity.financial_api.repository;

import com.trinity.financial_api.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByNumeroIdentificacion(String numeroIdentificacion);

    Optional<Cliente> findByEmail(String email);

    boolean existsByNumeroIdentificacion(String numeroIdentificacion);

    boolean existsByEmail(String email);
}
