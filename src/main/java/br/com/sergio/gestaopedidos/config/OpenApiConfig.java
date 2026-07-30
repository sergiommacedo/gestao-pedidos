package br.com.sergio.gestaopedidos.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()

                .info(new Info()

                        .title("Gestão de Pedidos API")

                        .version("1.0.0")

                        .description("""
                                API REST desenvolvida com Spring Boot para gerenciamento
                                de pedidos, clientes, produtos e usuários.
                                Projeto criado para fins de estudo e portfólio.
                                """)

                        .contact(new Contact()

                                .name("Sergio Murilo Macedo")

                                .email("sergio.murilo.macedo@gmail.com")

                        )
                )

                .externalDocs(
                        new ExternalDocumentation()

                                .description("Repositório GitHub")

                                .url("https://github.com/sergiommacedo")
                );
    }
}