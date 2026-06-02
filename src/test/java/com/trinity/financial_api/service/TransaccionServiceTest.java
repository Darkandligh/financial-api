package com.trinity.financial_api.service;

import com.trinity.financial_api.entity.Cliente;
import com.trinity.financial_api.entity.Producto;
import com.trinity.financial_api.entity.Transaccion;
import com.trinity.financial_api.enums.EstadoCuenta;
import com.trinity.financial_api.enums.TipoCuenta;
import com.trinity.financial_api.enums.TipoTransaccion;
import com.trinity.financial_api.exception.BusinessException;
import com.trinity.financial_api.repository.ProductoRepository;
import com.trinity.financial_api.repository.TransaccionRepository;
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
class TransaccionServiceTest {

    @Mock
    private TransaccionRepository transaccionRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private TransaccionService transaccionService;

    private Producto cuentaActiva;

    @BeforeEach
    void setUp() {
        cuentaActiva = Producto.builder()
            .id(1L)
            .numeroCuenta("5300000001")
            .tipoCuenta(TipoCuenta.AHORRO)
            .estado(EstadoCuenta.ACTIVA)
            .saldo(new BigDecimal("300000.00"))
            .cliente(Cliente.builder().id(1L).build())
            .build();
    }

    @Test
    void retirar_debeLanzarExcepcion_cuandoSaldoEsInsuficiente() {
        when(productoRepository.findByNumeroCuenta("5300000001"))
            .thenReturn(Optional.of(cuentaActiva));

        assertThatThrownBy(() ->
            transaccionService.retirar("5300000001", new BigDecimal("500000.00"))
        )
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Saldo insuficiente");

        verify(productoRepository, never()).save(any());
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void consignar_debeIncrementarSaldo_cuandoDatosValidos() {
        when(productoRepository.findByNumeroCuenta("5300000001"))
            .thenReturn(Optional.of(cuentaActiva));

        Transaccion transaccionGuardada = Transaccion.builder()
            .id(1L)
            .tipoTransaccion(TipoTransaccion.DEPOSITO)
            .monto(new BigDecimal("200000.00"))
            .cuentaOrigen(cuentaActiva)
            .build();

        when(transaccionRepository.save(any())).thenReturn(transaccionGuardada);

        Transaccion resultado = transaccionService.consignar("5300000001", new BigDecimal("200000.00"));

        assertThat(cuentaActiva.getSaldo()).isEqualByComparingTo("500000.00");
        assertThat(resultado.getTipoTransaccion()).isEqualTo(TipoTransaccion.DEPOSITO);
        verify(productoRepository).save(cuentaActiva);
        verify(transaccionRepository).save(any());
    }

    @Test
    void transferir_debeLanzarExcepcion_cuandoCuentasOriginYDestinoSonIguales() {
        assertThatThrownBy(() ->
            transaccionService.transferir("5300000001", "5300000001", new BigDecimal("100000.00"))
        )
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("no pueden ser la misma");

        verify(productoRepository, never()).findByNumeroCuenta(any());
    }
}
