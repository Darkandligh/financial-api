package com.trinity.financial_api.controller;

import com.trinity.financial_api.dto.EstadoRequest;
import com.trinity.financial_api.dto.ProductoRequest;
import com.trinity.financial_api.dto.ProductoResponse;
import com.trinity.financial_api.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<ProductoResponse>> listarPorCliente(@PathVariable Long clienteId) {
        List<ProductoResponse> productos = productoService.listarPorCliente(clienteId)
            .stream()
            .map(ProductoResponse::from)
            .toList();
        return ResponseEntity.ok(productos);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse response = ProductoResponse.from(
            productoService.crearProducto(request.clienteId(), request.tipoCuenta(), request.exentaGMF())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ProductoResponse> cambiarEstado(@PathVariable Long id,
                                                          @Valid @RequestBody EstadoRequest request) {
        ProductoResponse response = ProductoResponse.from(
            productoService.cambiarEstado(id, request.estado())
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<ProductoResponse> cancelar(@PathVariable Long id) {
        ProductoResponse response = ProductoResponse.from(productoService.cancelar(id));
        return ResponseEntity.ok(response);
    }
}
