package com.trinity.financial_api.entity;

import com.trinity.financial_api.enums.TipoTransaccion;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transacciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "El tipo de transacción es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_transaccion", nullable = false, length = 20)
    private TipoTransaccion tipoTransaccion;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Column(name = "monto", nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    // Siempre requerida: en DEPOSITO/RETIRO es la cuenta afectada; en TRANSFERENCIA es el origen
    @NotNull(message = "La cuenta origen es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_origen_id", nullable = false)
    private Producto cuentaOrigen;

    // Solo requerida en TRANSFERENCIA
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_destino_id", nullable = true)
    private Producto cuentaDestino;

    @PrePersist
    protected void onCreate() {
        fecha = LocalDateTime.now();
    }
}
