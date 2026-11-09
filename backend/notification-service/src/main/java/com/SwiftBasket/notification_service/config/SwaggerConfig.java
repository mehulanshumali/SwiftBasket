package com.SwiftBasket.notification_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftBasket Notification Service API")
                        .version("1.0.0")
                        .description("Notification Service for SwiftBasket E-commerce Platform - "
                                + "event-driven emails (welcome, order confirmation, payment receipt, status updates) "
                                + "with a per-user audit trail")
                        .contact(new Contact()
                                .name("SwiftBasket Team")
                                .email("support@swiftbasket.com")
                                .url("https://swiftbasket.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .components(new Components());
    }
}
