package com.trinity.financial_api.controller;

import com.trinity.financial_api.dto.ProductoRequest;
import com.trinity.financial_api.dto.ProductoResponse;
import com.trinity.financial_api.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse response = ProductoResponse.from(
            productoService.crearProducto(request.clienteId(), request.tipoCuenta(), request.exentaGMF())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
