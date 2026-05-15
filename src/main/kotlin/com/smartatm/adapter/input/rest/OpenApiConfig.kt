package com.smartatm.adapter.input.rest

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Smart ATM API")
                .version("1.0.0")
                .description("API de caixa eletrônico inteligente com IA")
                .contact(
                    Contact()
                        .name("Smart ATM")
                )
        )
}
