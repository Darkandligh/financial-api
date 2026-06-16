package com.trinity.financial_api.service;

import com.trinity.financial_api.entity.Producto;
import com.trinity.financial_api.entity.Transaccion;
import com.trinity.financial_api.enums.EstadoCuenta;
import com.trinity.financial_api.enums.TipoTransaccion;
import com.trinity.financial_api.exception.BusinessException;
import com.trinity.financial_api.repository.ProductoRepository;
import com.trinity.financial_api.repository.TransaccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final ProductoRepository productoRepository;

    public TransaccionService(TransaccionRepository transaccionRepository,
                               ProductoRepository productoRepository) {
        this.transaccionRepository = transaccionRepository;
        this.productoRepository = productoRepository;
    }

    public List<Transaccion> listarPorCuenta(String numeroCuenta) {
        productoRepository.findByNumeroCuenta(numeroCuenta)
            .orElseThrow(() -> new BusinessException("No existe una cuenta con número: " + numeroCuenta));
        return transaccionRepository.findByCuentaNumero(numeroCuenta);
    }

    @Transactional
    public Transaccion consignar(String numeroCuenta, BigDecimal monto) {
        Producto cuenta = buscarCuentaActiva(numeroCuenta);

        cuenta.setSaldo(cuenta.getSaldo().add(monto));
        productoRepository.save(cuenta);

        return transaccionRepository.save(Transaccion.builder()
            .tipoTransaccion(TipoTransaccion.DEPOSITO)
            .monto(monto)
            .cuentaOrigen(cuenta)
            .build());
    }

    @Transactional
    public Transaccion retirar(String numeroCuenta, BigDecimal monto) {
        Producto cuenta = buscarCuentaActiva(numeroCuenta);

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new BusinessException("Saldo insuficiente. Saldo disponible: " + cuenta.getSaldo());
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        productoRepository.save(cuenta);

        return transaccionRepository.save(Transaccion.builder()
            .tipoTransaccion(TipoTransaccion.RETIRO)
            .monto(monto)
            .cuentaOrigen(cuenta)
            .build());
    }

    @Transactional
    public Transaccion transferir(String numeroOrigen, String numeroDestino, BigDecimal monto) {
        if (numeroOrigen.equals(numeroDestino)) {
            throw new BusinessException("La cuenta origen y destino no pueden ser la misma");
        }

        Producto origen = buscarCuentaActiva(numeroOrigen);
        Producto destino = buscarCuentaActiva(numeroDestino);

        if (origen.getSaldo().compareTo(monto) < 0) {
            throw new BusinessException("Saldo insuficiente. Saldo disponible: " + origen.getSaldo());
        }

        origen.setSaldo(origen.getSaldo().subtract(monto));
        destino.setSaldo(destino.getSaldo().add(monto));
        productoRepository.save(origen);
        productoRepository.save(destino);

        transaccionRepository.save(Transaccion.builder()
            .tipoTransaccion(TipoTransaccion.DEBITO)
            .monto(monto)
            .cuentaOrigen(origen)
            .cuentaDestino(destino)
            .build());

        return transaccionRepository.save(Transaccion.builder()
            .tipoTransaccion(TipoTransaccion.CREDITO)
            .monto(monto)
            .cuentaOrigen(destino)
            .cuentaDestino(origen)
            .build());
    }

    private Producto buscarCuentaActiva(String numeroCuenta) {
        Producto cuenta = productoRepository.findByNumeroCuenta(numeroCuenta)
            .orElseThrow(() -> new BusinessException("No existe una cuenta con número: " + numeroCuenta));

        if (cuenta.getEstado() != EstadoCuenta.ACTIVA) {
            throw new BusinessException("La cuenta " + numeroCuenta + " no está activa");
        }

        return cuenta;
    }
}
