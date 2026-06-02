package com.trinity.financial_api.controller;

import com.trinity.financial_api.entity.Cliente;
import com.trinity.financial_api.entity.Producto;
import com.trinity.financial_api.enums.EstadoCuenta;
import com.trinity.financial_api.enums.TipoCuenta;
import com.trinity.financial_api.exception.BusinessException;
import com.trinity.financial_api.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductoController.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductoService productoService;

    private Producto productoEjemplo;

    @BeforeEach
    void setUp() {
        productoEjemplo = Producto.builder()
            .id(1L)
            .tipoCuenta(TipoCuenta.AHORRO)
            .numeroCuenta("5300000001")
            .estado(EstadoCuenta.ACTIVA)
            .saldo(BigDecimal.ZERO)
            .exentaGMF(false)
            .cliente(Cliente.builder().id(1L).build())
            .fechaCreacion(LocalDateTime.now())
            .build();
    }

    @Test
    void crear_debeRetornar201_cuandoDatosValidos() throws Exception {
        when(productoService.crearProducto(any(), any(), any(Boolean.class))).thenReturn(productoEjemplo);

        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "clienteId": 1,
                        "tipoCuenta": "AHORRO",
                        "exentaGMF": false
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numeroCuenta").value("5300000001"))
            .andExpect(jsonPath("$.estado").value("ACTIVA"))
            .andExpect(jsonPath("$.tipoCuenta").value("AHORRO"));
    }

    @Test
    void cambiarEstado_debeRetornar200_cuandoEstadoValido() throws Exception {
        Producto productoInactivo = Producto.builder()
            .id(1L)
            .tipoCuenta(TipoCuenta.AHORRO)
            .numeroCuenta("5300000001")
            .estado(EstadoCuenta.INACTIVA)
            .saldo(BigDecimal.ZERO)
            .exentaGMF(false)
            .cliente(Cliente.builder().id(1L).build())
            .fechaCreacion(LocalDateTime.now())
            .build();

        when(productoService.cambiarEstado(eq(1L), eq(EstadoCuenta.INACTIVA))).thenReturn(productoInactivo);

        mockMvc.perform(patch("/api/productos/1/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "estado": "INACTIVA" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INACTIVA"));
    }

    @Test
    void cancelar_debeRetornar400_cuandoCuentaTieneSaldo() throws Exception {
        doThrow(new BusinessException("Solo se pueden cancelar cuentas con saldo igual a $0. Saldo actual: 150000.00"))
            .when(productoService).cancelar(1L);

        mockMvc.perform(post("/api/productos/1/cancelar"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("Solo se pueden cancelar cuentas con saldo igual a $0. Saldo actual: 150000.00"));
    }

    @Test
    void cancelar_debeRetornar200_cuandoCuentaTieneSaldoCero() throws Exception {
        Producto productoCancelado = Producto.builder()
            .id(1L)
            .tipoCuenta(TipoCuenta.AHORRO)
            .numeroCuenta("5300000001")
            .estado(EstadoCuenta.CANCELADA)
            .saldo(BigDecimal.ZERO)
            .exentaGMF(false)
            .cliente(Cliente.builder().id(1L).build())
            .fechaCreacion(LocalDateTime.now())
            .build();

        when(productoService.cancelar(1L)).thenReturn(productoCancelado);

        mockMvc.perform(post("/api/productos/1/cancelar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }
}
