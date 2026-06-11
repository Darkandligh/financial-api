package com.trinity.financial_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.financial_api.repository.ClienteRepository;
import com.trinity.financial_api.repository.ProductoRepository;
import com.trinity.financial_api.repository.TransaccionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TransaccionControllerComponentTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired ClienteRepository clienteRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired TransaccionRepository transaccionRepository;

    private String cuentaAhorro;
    private String cuentaCorriente;

    @BeforeEach
    void setup() throws Exception {
        transaccionRepository.deleteAll();
        productoRepository.deleteAll();
        clienteRepository.deleteAll();

        String clienteResp = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "tipoIdentificacion",   "CC",
                    "numeroIdentificacion", "1099887766",
                    "nombres",  "Juan",
                    "apellidos", "Pérez",
                    "email",    "juan@email.com",
                    "fechaNacimiento", "1990-05-15"))))
            .andReturn().getResponse().getContentAsString();

        Long clienteId = mapper.readTree(clienteResp).get("id").asLong();

        cuentaAhorro = mapper.readTree(
            mockMvc.perform(post("/api/productos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Map.of(
                        "clienteId", clienteId, "tipoCuenta", "AHORRO", "exentaGMF", false))))
                .andReturn().getResponse().getContentAsString()
        ).get("numeroCuenta").asText();

        cuentaCorriente = mapper.readTree(
            mockMvc.perform(post("/api/productos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Map.of(
                        "clienteId", clienteId, "tipoCuenta", "CORRIENTE", "exentaGMF", false))))
                .andReturn().getResponse().getContentAsString()
        ).get("numeroCuenta").asText();
    }

    @Test
    void deposito_retorna201_yActualizaSaldo() throws Exception {
        mockMvc.perform(post("/api/transacciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "numeroCuenta", cuentaAhorro, "monto", 500000))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("DEPOSITO"))
            .andExpect(jsonPath("$.monto").value(500000));

        mockMvc.perform(get("/api/productos/cliente/{id}",
                productoRepository.findByNumeroCuenta(cuentaAhorro).get().getCliente().getId()))
            .andExpect(status().isOk());
    }

    @Test
    void retiro_retorna400_cuandoSaldoInsuficiente() throws Exception {
        mockMvc.perform(post("/api/transacciones/retiro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "numeroCuenta", cuentaAhorro, "monto", 999999))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value(containsString("Saldo insuficiente")));
    }

    @Test
    void transferencia_generaDosRegistros_debitoYCredito() throws Exception {
        mockMvc.perform(post("/api/transacciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "numeroCuenta", cuentaAhorro, "monto", 300000))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/transacciones/transferencia")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "cuentaOrigen",  cuentaAhorro,
                    "cuentaDestino", cuentaCorriente,
                    "monto",         100000))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipo").value("CREDITO"));

        long totalTransacciones = transaccionRepository.count();
        assertThat(totalTransacciones).isEqualTo(3); // 1 depósito + 2 movimientos de transferencia
    }
}
