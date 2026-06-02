package com.trinity.financial_api.dto;

import com.trinity.financial_api.enums.EstadoCuenta;
import jakarta.validation.constraints.NotNull;

public record EstadoRequest(

    @NotNull(message = "El estado es obligatorio")
    EstadoCuenta estado
) {}
