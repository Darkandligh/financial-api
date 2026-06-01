package com.trinity.financial_api.controller;

import com.trinity.financial_api.dto.TransaccionResponse;
import com.trinity.financial_api.dto.TransaccionSimpleRequest;
import com.trinity.financial_api.dto.TransferenciaRequest;
import com.trinity.financial_api.service.TransaccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @PostMapping("/deposito")
    public ResponseEntity<TransaccionResponse> depositar(@Valid @RequestBody TransaccionSimpleRequest request) {
        TransaccionResponse response = TransaccionResponse.from(
            transaccionService.consignar(request.numeroCuenta(), request.monto())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/retiro")
    public ResponseEntity<TransaccionResponse> retirar(@Valid @RequestBody TransaccionSimpleRequest request) {
        TransaccionResponse response = TransaccionResponse.from(
            transaccionService.retirar(request.numeroCuenta(), request.monto())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/transferencia")
    public ResponseEntity<TransaccionResponse> transferir(@Valid @RequestBody TransferenciaRequest request) {
        TransaccionResponse response = TransaccionResponse.from(
            transaccionService.transferir(request.cuentaOrigen(), request.cuentaDestino(), request.monto())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
