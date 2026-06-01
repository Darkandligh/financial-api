package com.trinity.financial_api.service;

import com.trinity.financial_api.entity.Cliente;
import com.trinity.financial_api.entity.Producto;
import com.trinity.financial_api.enums.TipoCuenta;
import com.trinity.financial_api.exception.BusinessException;
import com.trinity.financial_api.repository.ClienteRepository;
import com.trinity.financial_api.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final Random random = new Random();

    public ProductoService(ProductoRepository productoRepository, ClienteRepository clienteRepository) {
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Producto crearProducto(Long clienteId, TipoCuenta tipoCuenta, boolean exentaGMF) {
        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new BusinessException("No existe un cliente con id: " + clienteId));

        Producto producto = Producto.builder()
            .tipoCuenta(tipoCuenta)
            .numeroCuenta(generarNumeroCuenta(tipoCuenta))
            .exentaGMF(exentaGMF)
            .cliente(cliente)
            .build();

        return productoRepository.save(producto);
    }

    // Prefijo 53 para AHORRO, 33 para CORRIENTE + 8 dígitos aleatorios = 10 dígitos totales
    // Reintenta si el número ya existe en la BD (colisión improbable pero posible)
    private String generarNumeroCuenta(TipoCuenta tipoCuenta) {
        String prefijo = tipoCuenta == TipoCuenta.AHORRO ? "53" : "33";
        String numero;
        do {
            numero = prefijo + String.format("%08d", random.nextInt(100_000_000));
        } while (productoRepository.existsByNumeroCuenta(numero));
        return numero;
    }
}
