package com.trinity.financial_api.service;

import com.trinity.financial_api.entity.Cliente;
import com.trinity.financial_api.entity.Producto;
import com.trinity.financial_api.enums.EstadoCuenta;
import com.trinity.financial_api.enums.TipoCuenta;
import com.trinity.financial_api.exception.BusinessException;
import com.trinity.financial_api.repository.ClienteRepository;
import com.trinity.financial_api.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ProductoService productoService;

    private Cliente clienteEjemplo;
    private Producto productoActivo;

    @BeforeEach
    void setUp() {
        clienteEjemplo = Cliente.builder()
            .id(1L)
            .nombres("Juan")
            .apellidos("Perez")
            .build();

        productoActivo = Producto.builder()
            .id(1L)
            .tipoCuenta(TipoCuenta.AHORRO)
            .numeroCuenta("5300000001")
            .estado(EstadoCuenta.ACTIVA)
            .saldo(BigDecimal.ZERO)
            .cliente(clienteEjemplo)
            .build();
    }

    @Test
    void crearProducto_debeLanzarExcepcion_cuandoClienteNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.crearProducto(99L, TipoCuenta.AHORRO, false))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("No existe un cliente");

        verify(productoRepository, never()).save(any());
    }

    @Test
    void crearProducto_debeGenerarNumeroCuentaConPrefijo53_cuandoTipoAhorro() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEjemplo));
        when(productoRepository.existsByNumeroCuenta(any())).thenReturn(false);
        when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Producto resultado = productoService.crearProducto(1L, TipoCuenta.AHORRO, false);

        assertThat(resultado.getNumeroCuenta()).startsWith("53");
        assertThat(resultado.getNumeroCuenta()).hasSize(10);
    }

    @Test
    void crearProducto_debeGenerarNumeroCuentaConPrefijo33_cuandoTipoCorriente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEjemplo));
        when(productoRepository.existsByNumeroCuenta(any())).thenReturn(false);
        when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Producto resultado = productoService.crearProducto(1L, TipoCuenta.CORRIENTE, false);

        assertThat(resultado.getNumeroCuenta()).startsWith("33");
        assertThat(resultado.getNumeroCuenta()).hasSize(10);
    }

    @Test
    void cancelar_debeLanzarExcepcion_cuandoSaldoNoEsCero() {
        productoActivo.setSaldo(new BigDecimal("150000.00"));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoActivo));

        assertThatThrownBy(() -> productoService.cancelar(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("saldo igual a $0");

        verify(productoRepository, never()).save(any());
    }

    @Test
    void cancelar_debeLanzarExcepcion_cuandoCuentaYaEstaCancelada() {
        productoActivo.setEstado(EstadoCuenta.CANCELADA);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoActivo));

        assertThatThrownBy(() -> productoService.cancelar(1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ya está cancelada");
    }

    @Test
    void cancelar_debeCambiarEstadoACancelada_cuandoSaldoEsCero() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoActivo));
        when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Producto resultado = productoService.cancelar(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoCuenta.CANCELADA);
        verify(productoRepository).save(productoActivo);
    }

    @Test
    void cambiarEstado_debeLanzarExcepcion_cuandoCuentaEstaCancelada() {
        productoActivo.setEstado(EstadoCuenta.CANCELADA);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoActivo));

        assertThatThrownBy(() -> productoService.cambiarEstado(1L, EstadoCuenta.ACTIVA))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("cancelada");
    }

    @Test
    void cambiarEstado_debeLanzarExcepcion_cuandoSeIntentaCancelarPorEsteEndpoint() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoActivo));

        assertThatThrownBy(() -> productoService.cambiarEstado(1L, EstadoCuenta.CANCELADA))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("/cancelar");
    }

    @Test
    void listarPorCliente_debeLanzarExcepcion_cuandoClienteNoExiste() {
        when(clienteRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productoService.listarPorCliente(99L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("No existe un cliente");
    }
}
