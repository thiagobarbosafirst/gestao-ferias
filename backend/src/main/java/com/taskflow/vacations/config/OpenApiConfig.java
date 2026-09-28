package com.taskflow.vacations.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados da documentação Swagger/OpenAPI (http://localhost:8080/swagger-ui.html).
 * Define o esquema "bearerAuth" para que o botão "Authorize" do Swagger aceite o token JWT.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "TaskFlow - Gestão de Férias API", version = "1.0",
                description = "API para gestão de colaboradores e pedidos de férias. "
                        + "Faça POST /api/auth/login, copie o token e use o botão Authorize."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
