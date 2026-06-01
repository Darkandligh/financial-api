package com.trinity.financial_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransaccionSimpleRequest(

    @NotBlank(message = "El número de cuenta es obligatorio")
    String numeroCuenta,

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    BigDecimal monto
) {}
