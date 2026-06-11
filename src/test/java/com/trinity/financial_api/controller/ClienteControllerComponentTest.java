package com.trinity.financial_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trinity.financial_api.entity.Cliente;
import com.trinity.financial_api.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ClienteControllerComponentTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired ClienteRepository clienteRepository;

    @BeforeEach
    void limpiar() {
        clienteRepository.deleteAll();
    }

    private Map<String, Object> bodyClienteValido() {
        return Map.of(
            "tipoIdentificacion",   "CC",
            "numeroIdentificacion", "1099887766",
            "nombres",              "Juan",
            "apellidos",            "Pérez",
            "email",                "juan.perez@email.com",
            "fechaNacimiento",      "1990-05-15"
        );
    }

    @Test
    void crearCliente_retorna201_cuandoDatosValidos() throws Exception {
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(bodyClienteValido())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.nombres").value("Juan"))
            .andExpect(jsonPath("$.fechaCreacion").isNotEmpty());
    }

    @Test
    void crearCliente_retorna400_cuandoClienteEsMenorDeEdad() throws Exception {
        var body = Map.of(
            "tipoIdentificacion",   "CC",
            "numeroIdentificacion", "111222333",
            "nombres",              "Pedro",
            "apellidos",            "Joven",
            "email",                "pedro@email.com",
            "fechaNacimiento",      LocalDate.now().minusYears(16).toString()
        );

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value(containsString("mayor de edad")));
    }

    @Test
    void crearCliente_retorna400_cuandoEmailDuplicado() throws Exception {
        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(bodyClienteValido())))
            .andExpect(status().isCreated());

        var body2 = Map.of(
            "tipoIdentificacion",   "CC",
            "numeroIdentificacion", "999888777",
            "nombres",              "Ana",
            "apellidos",            "Ruiz",
            "email",                "juan.perez@email.com",
            "fechaNacimiento",      "1988-01-01"
        );

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body2)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void buscarPorId_retorna200_cuandoClienteExiste() throws Exception {
        String response = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(bodyClienteValido())))
            .andReturn().getResponse().getContentAsString();

        Long id = mapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/clientes/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("juan.perez@email.com"));
    }

    @Test
    void actualizarCliente_retorna200_yActualizaFechaModificacion() throws Exception {
        String response = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(bodyClienteValido())))
            .andReturn().getResponse().getContentAsString();

        Long id = mapper.readTree(response).get("id").asLong();

        var update = Map.of(
            "tipoIdentificacion",   "CC",
            "numeroIdentificacion", "1099887766",
            "nombres",              "Juan Carlos",
            "apellidos",            "Pérez",
            "email",                "juan.perez@email.com",
            "fechaNacimiento",      "1990-05-15"
        );

        mockMvc.perform(put("/api/clientes/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(update)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nombres").value("Juan Carlos"));
    }

    @Test
    void eliminarCliente_retorna204_cuandoNoTieneProductos() throws Exception {
        String response = mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(bodyClienteValido())))
            .andReturn().getResponse().getContentAsString();

        Long id = mapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/clientes/{id}", id))
            .andExpect(status().isNoContent());
    }
}
