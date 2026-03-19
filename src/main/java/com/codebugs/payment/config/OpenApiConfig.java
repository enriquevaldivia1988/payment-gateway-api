package com.codebugs.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentGatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Gateway API")
                        .description("PCI DSS-compliant payment gateway supporting card authorization, "
                                + "capture, reversal, and refund operations for multi-tenant e-commerce platforms.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Enrique Valdivia Rios")
                                .url("https://github.com/enriquevaldivia1988"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("https://api.example.com").description("Production")
                ));
    }
}
