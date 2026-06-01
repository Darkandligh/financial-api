package com.trinity.financial_api.dto;

import com.trinity.financial_api.entity.Transaccion;
import com.trinity.financial_api.enums.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccionResponse(
    Long id,
    TipoTransaccion tipo,
    BigDecimal monto,
    LocalDateTime fecha,
    String numeroCuentaOrigen,
    String numeroCuentaDestino
) {
    public static TransaccionResponse from(Transaccion t) {
        return new TransaccionResponse(
            t.getId(),
            t.getTipoTransaccion(),
            t.getMonto(),
            t.getFecha(),
            t.getCuentaOrigen().getNumeroCuenta(),
            t.getCuentaDestino() != null ? t.getCuentaDestino().getNumeroCuenta() : null
        );
    }
}
