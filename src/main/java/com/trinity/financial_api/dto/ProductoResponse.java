package com.trinity.financial_api.dto;

import com.trinity.financial_api.entity.Producto;
import com.trinity.financial_api.enums.EstadoCuenta;
import com.trinity.financial_api.enums.TipoCuenta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
    Long id,
    TipoCuenta tipoCuenta,
    String numeroCuenta,
    EstadoCuenta estado,
    BigDecimal saldo,
    boolean exentaGMF,
    Long clienteId,
    LocalDateTime fechaCreacion
) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(
            p.getId(),
            p.getTipoCuenta(),
            p.getNumeroCuenta(),
            p.getEstado(),
            p.getSaldo(),
            p.isExentaGMF(),
            p.getCliente().getId(),
            p.getFechaCreacion()
        );
    }
}
