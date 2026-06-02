package com.trinity.financial_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financialApiOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Financial API - MiniBanco")
                .description("API REST para gestión de clientes, cuentas y transacciones bancarias")
                .version("1.0.0"));
    }
}
