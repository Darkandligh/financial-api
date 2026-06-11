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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductoControllerComponentTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired ClienteRepository clienteRepository;
    @Autowired ProductoRepository productoRepository;
    @Autowired TransaccionRepository transaccionRepository;

    private Long clienteId;

    @BeforeEach
    void setup() throws Exception {
        transaccionRepository.deleteAll();
        productoRepository.deleteAll();
        clienteRepository.deleteAll();

        var body = Map.of(
            "tipoIdentificacion",   "CC",
            "numeroIdentificacion", "1099887766",
            "nombres",              "Juan",
            "apellidos",            "Pérez",
            "email",                "juan@email.com",
            "fechaNacimiento",      "1990-05-15"
        );

        String resp = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andReturn().getResponse().getContentAsString();

        clienteId = mapper.readTree(resp).get("id").asLong();
    }

    @Test
    void crearCuentaAhorro_retorna201_yNumeroCuentaIniciaEn53() throws Exception {
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "clienteId", clienteId,
                    "tipoCuenta", "AHORRO",
                    "exentaGMF", false))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipoCuenta").value("AHORRO"))
            .andExpect(jsonPath("$.numeroCuenta").value(startsWith("53")))
            .andExpect(jsonPath("$.estado").value("ACTIVA"))
            .andExpect(jsonPath("$.saldo").value(0));
    }

    @Test
    void crearCuentaCorriente_retorna201_yNumeroCuentaIniciaEn33() throws Exception {
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "clienteId", clienteId,
                    "tipoCuenta", "CORRIENTE",
                    "exentaGMF", true))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tipoCuenta").value("CORRIENTE"))
            .andExpect(jsonPath("$.numeroCuenta").value(startsWith("33")));
    }

    @Test
    void listarCuentas_retorna200_conLasCuentasDelCliente() throws Exception {
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "clienteId", clienteId, "tipoCuenta", "AHORRO", "exentaGMF", false))))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/productos/cliente/{id}", clienteId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cancelarCuenta_retorna400_cuandoSaldoDistintoDeCero() throws Exception {
        String resp = mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "clienteId", clienteId, "tipoCuenta", "AHORRO", "exentaGMF", false))))
            .andReturn().getResponse().getContentAsString();

        Long productoId = mapper.readTree(resp).get("id").asLong();
        String numeroCuenta = mapper.readTree(resp).get("numeroCuenta").asText();

        mockMvc.perform(post("/api/transacciones/deposito")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "numeroCuenta", numeroCuenta, "monto", 100000))))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/productos/{id}/cancelar", productoId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value(containsString("saldo igual a $0")));
    }
}
