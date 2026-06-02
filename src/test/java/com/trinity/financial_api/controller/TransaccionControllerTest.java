package com.trinity.financial_api.controller;

import com.trinity.financial_api.entity.Cliente;
import com.trinity.financial_api.entity.Producto;
import com.trinity.financial_api.entity.Transaccion;
import com.trinity.financial_api.enums.EstadoCuenta;
import com.trinity.financial_api.enums.TipoCuenta;
import com.trinity.financial_api.enums.TipoTransaccion;
import com.trinity.financial_api.exception.BusinessException;
import com.trinity.financial_api.service.TransaccionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransaccionController.class)
class TransaccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransaccionService transaccionService;

    private Transaccion transaccionEjemplo;

    @BeforeEach
    void setUp() {
        Producto cuenta = Producto.builder()
            .id(1L)
            .numeroCuenta("5300000001")
            .tipoCuenta(TipoCuenta.AHORRO)
            .estado(EstadoCuenta.ACTIVA)
            .saldo(new BigDecimal("500000.00"))
            .cliente(Cliente.builder().id(1L).build())
            .build();

        transaccionEjemplo = Transaccion.builder()
            .id(1L)
            .tipoTransaccion(TipoTransaccion.DEPOSITO)
            .monto(new BigDecimal("200000.00"))
            .cuentaOrigen(cuenta)
            .build();
    }

    @Test
    void depositar_debeRetornar201_cuandoDatosValidos() throws Exception {
        when(transaccionService.consignar(any(), any())).thenReturn(transaccionEjemplo);

        mockMvc.perform(post("/api/transacciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "numeroCuenta": "5300000001",
                        "monto": 200000.00
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("DEPOSITO"))
            .andExpect(jsonPath("$.numeroCuentaOrigen").value("5300000001"));
    }

    @Test
    void retirar_debeRetornar400_cuandoSaldoInsuficiente() throws Exception {
        when(transaccionService.retirar(any(), any()))
            .thenThrow(new BusinessException("Saldo insuficiente. Saldo disponible: 200000.00"));

        mockMvc.perform(post("/api/transacciones/retiro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "numeroCuenta": "5300000001",
                        "monto": 999999.00
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("Saldo insuficiente. Saldo disponible: 200000.00"));
    }

    @Test
    void transferir_debeRetornar400_cuandoCuentasIguales() throws Exception {
        when(transaccionService.transferir(any(), any(), any()))
            .thenThrow(new BusinessException("La cuenta origen y destino no pueden ser la misma"));

        mockMvc.perform(post("/api/transacciones/transferencia")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "cuentaOrigen": "5300000001",
                        "cuentaDestino": "5300000001",
                        "monto": 50000.00
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("La cuenta origen y destino no pueden ser la misma"));
    }
}
