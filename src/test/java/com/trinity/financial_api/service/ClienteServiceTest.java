package com.trinity.financial_api.service;

import com.trinity.financial_api.entity.Cliente;
import com.trinity.financial_api.exception.BusinessException;
import com.trinity.financial_api.repository.ClienteRepository;
import com.trinity.financial_api.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente clienteValido;

    @BeforeEach
    void setUp() {
        clienteValido = Cliente.builder()
            .tipoIdentificacion("CC")
            .numeroIdentificacion("1099887766")
            .nombres("Juan")
            .apellidos("Perez")
            .email("juan@email.com")
            .fechaNacimiento(LocalDate.of(1990, 5, 15))
            .build();
    }

    @Test
    void crearCliente_debeLanzarExcepcion_cuandoClienteEsMenorDeEdad() {
        clienteValido.setFechaNacimiento(LocalDate.now().minusYears(17));

        assertThatThrownBy(() -> clienteService.crearCliente(clienteValido))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("mayor de edad");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crearCliente_debeLanzarExcepcion_cuandoNumeroIdentificacionYaExiste() {
        when(clienteRepository.existsByNumeroIdentificacion("1099887766")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crearCliente(clienteValido))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("número de identificación");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crearCliente_debeLanzarExcepcion_cuandoEmailYaExiste() {
        when(clienteRepository.existsByNumeroIdentificacion(any())).thenReturn(false);
        when(clienteRepository.existsByEmail("juan@email.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crearCliente(clienteValido))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("email");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crearCliente_debeGuardar_cuandoDatosValidos() {
        when(clienteRepository.existsByNumeroIdentificacion(any())).thenReturn(false);
        when(clienteRepository.existsByEmail(any())).thenReturn(false);
        when(clienteRepository.save(any())).thenReturn(clienteValido);

        Cliente resultado = clienteService.crearCliente(clienteValido);

        assertThat(resultado).isNotNull();
        verify(clienteRepository).save(clienteValido);
    }

    @Test
    void eliminar_debeLanzarExcepcion_cuandoClienteNoExiste() {
        when(clienteRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> clienteService.eliminar(99L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("No existe un cliente");

        verify(clienteRepository, never()).deleteById(any());
    }

    @Test
    void eliminar_debeLanzarExcepcion_cuandoClienteTieneProductos() {
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(productoRepository.existsByClienteId(1L)).thenReturn(true);

        assertThatThrownBy(() -> clienteService.eliminar(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("productos vinculados");

        verify(clienteRepository, never()).deleteById(any());
    }

    @Test
    void eliminar_debeEliminar_cuandoClienteExisteYNoTieneProductos() {
        when(clienteRepository.existsById(1L)).thenReturn(true);
        when(productoRepository.existsByClienteId(1L)).thenReturn(false);

        clienteService.eliminar(1L);

        verify(clienteRepository).deleteById(1L);
    }

    @Test
    void actualizar_debeLanzarExcepcion_cuandoClienteNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.actualizar(99L, clienteValido))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("No existe un cliente");
    }

    @Test
    void actualizar_debeLanzarExcepcion_cuandoNuevoEmailYaExiste() {
        Cliente existente = Cliente.builder()
            .id(1L)
            .tipoIdentificacion("CC")
            .numeroIdentificacion("1099887766")
            .nombres("Juan")
            .apellidos("Perez")
            .email("viejo@email.com")
            .fechaNacimiento(LocalDate.of(1990, 5, 15))
            .build();

        clienteValido.setEmail("nuevo@email.com");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByEmail("nuevo@email.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.actualizar(1L, clienteValido))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("email");
    }
}
