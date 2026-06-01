package com.trinity.financial_api.service;

import com.trinity.financial_api.entity.Cliente;
import com.trinity.financial_api.exception.BusinessException;
import com.trinity.financial_api.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Cliente crearCliente(Cliente cliente) {
        validarMayoriaDeEdad(cliente.getFechaNacimiento());
        validarNumeroIdentificacionUnico(cliente.getNumeroIdentificacion());
        validarEmailUnico(cliente.getEmail());
        return clienteRepository.save(cliente);
    }

    // Un cliente nacido después de hoy-18años no ha cumplido 18 todavía
    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        if (fechaNacimiento.isAfter(LocalDate.now().minusYears(18))) {
            throw new BusinessException("El cliente debe ser mayor de edad (18 años o más)");
        }
    }

    private void validarNumeroIdentificacionUnico(String numeroIdentificacion) {
        if (clienteRepository.existsByNumeroIdentificacion(numeroIdentificacion)) {
            throw new BusinessException(
                "Ya existe un cliente con el número de identificación: " + numeroIdentificacion
            );
        }
    }

    private void validarEmailUnico(String email) {
        if (clienteRepository.existsByEmail(email)) {
            throw new BusinessException(
                "Ya existe un cliente registrado con el email: " + email
            );
        }
    }
}
