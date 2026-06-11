package com.trinity.financial_api.service;

import com.trinity.financial_api.entity.Cliente;
import com.trinity.financial_api.exception.BusinessException;
import com.trinity.financial_api.repository.ClienteRepository;
import com.trinity.financial_api.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    public ClienteService(ClienteRepository clienteRepository, ProductoRepository productoRepository) {
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new BusinessException("No existe un cliente con id: " + id));
    }

    public Cliente buscarPorIdentificacion(String numeroIdentificacion) {
        return clienteRepository.findByNumeroIdentificacion(numeroIdentificacion)
            .orElseThrow(() -> new BusinessException("No existe un cliente con identificación: " + numeroIdentificacion));
    }

    public Cliente crearCliente(Cliente cliente) {
        cliente.setId(null);
        validarMayoriaDeEdad(cliente.getFechaNacimiento());
        validarNumeroIdentificacionUnico(cliente.getNumeroIdentificacion());
        validarEmailUnico(cliente.getEmail());
        return clienteRepository.save(cliente);
    }

    public Cliente actualizar(Long id, Cliente datos) {
        Cliente existente = clienteRepository.findById(id)
            .orElseThrow(() -> new BusinessException("No existe un cliente con id: " + id));

        validarMayoriaDeEdad(datos.getFechaNacimiento());

        if (!existente.getNumeroIdentificacion().equals(datos.getNumeroIdentificacion())) {
            validarNumeroIdentificacionUnico(datos.getNumeroIdentificacion());
        }
        if (!existente.getEmail().equals(datos.getEmail())) {
            validarEmailUnico(datos.getEmail());
        }

        existente.setTipoIdentificacion(datos.getTipoIdentificacion());
        existente.setNumeroIdentificacion(datos.getNumeroIdentificacion());
        existente.setNombres(datos.getNombres());
        existente.setApellidos(datos.getApellidos());
        existente.setEmail(datos.getEmail());
        existente.setFechaNacimiento(datos.getFechaNacimiento());

        return clienteRepository.save(existente);
    }

    public void eliminar(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new BusinessException("No existe un cliente con id: " + id);
        }
        if (productoRepository.existsByClienteId(id)) {
            throw new BusinessException("No se puede eliminar el cliente porque tiene productos vinculados");
        }
        clienteRepository.deleteById(id);
    }

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



