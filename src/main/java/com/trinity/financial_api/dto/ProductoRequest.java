package com.trinity.financial_api.dto;

import com.trinity.financial_api.enums.TipoCuenta;
import jakarta.validation.constraints.NotNull;

public record ProductoRequest(

    @NotNull(message = "El id del cliente es obligatorio")
    Long clienteId,

    @NotNull(message = "El tipo de cuenta es obligatorio")
    TipoCuenta tipoCuenta,

    boolean exentaGMF
) {}
