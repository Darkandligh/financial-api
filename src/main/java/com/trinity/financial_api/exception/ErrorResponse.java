package com.trinity.financial_api.exception;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class ErrorResponse {

    private String mensaje;
    private int codigo;
    private Map<String, String> errores;
}
