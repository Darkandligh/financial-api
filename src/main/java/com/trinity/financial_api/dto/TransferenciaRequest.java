package com.trinity.financial_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferenciaRequest(

    @NotBlank(message = "La cuenta origen es obligatoria")
    String cuentaOrigen,

    @NotBlank(message = "La cuenta destino es obligatoria")
    String cuentaDestino,

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    BigDecimal monto
) {}
