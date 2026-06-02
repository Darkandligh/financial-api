package com.trinity.financial_api.controller;

import com.trinity.financial_api.entity.Cliente;
import com.trinity.financial_api.exception.BusinessException;
import com.trinity.financial_api.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    private Cliente clienteEjemplo;

    private static final String CLIENTE_JSON = """
        {
            "tipoIdentificacion": "CC",
            "numeroIdentificacion": "1099887766",
            "nombres": "Juan",
            "apellidos": "Perez",
            "email": "juan@email.com",
            "fechaNacimiento": "1990-05-15"
        }
        """;

    @BeforeEach
    void setUp() {
        clienteEjemplo = Cliente.builder()
            .id(1L)
            .tipoIdentificacion("CC")
            .numeroIdentificacion("1099887766")
            .nombres("Juan")
            .apellidos("Perez")
            .email("juan@email.com")
            .fechaNacimiento(LocalDate.of(1990, 5, 15))
            .fechaCreacion(LocalDateTime.now())
            .build();
    }

    @Test
    void crear_debeRetornar201_cuandoDatosValidos() throws Exception {
        when(clienteService.crearCliente(any())).thenReturn(clienteEjemplo);

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CLIENTE_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombres").value("Juan"))
            .andExpect(jsonPath("$.email").value("juan@email.com"));
    }

    @Test
    void crear_debeRetornar400_cuandoClienteEsMenorDeEdad() throws Exception {
        when(clienteService.crearCliente(any()))
            .thenThrow(new BusinessException("El cliente debe ser mayor de edad (18 años o más)"));

        mockMvc.perform(post("/api/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CLIENTE_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("El cliente debe ser mayor de edad (18 años o más)"));
    }

    @Test
    void actualizar_debeRetornar200_cuandoDatosValidos() throws Exception {
        when(clienteService.actualizar(eq(1L), any())).thenReturn(clienteEjemplo);

        mockMvc.perform(put("/api/clientes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CLIENTE_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nombres").value("Juan"));
    }

    @Test
    void eliminar_debeRetornar204_cuandoClienteNoTieneProductos() throws Exception {
        doNothing().when(clienteService).eliminar(1L);

        mockMvc.perform(delete("/api/clientes/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_debeRetornar400_cuandoClienteTieneProductosVinculados() throws Exception {
        doThrow(new BusinessException("No se puede eliminar el cliente porque tiene productos vinculados"))
            .when(clienteService).eliminar(1L);

        mockMvc.perform(delete("/api/clientes/1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("No se puede eliminar el cliente porque tiene productos vinculados"));
    }
}
