package it.fin8.grdsheet.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("GRD Sheet API")
                        .description("Documentazione delle API per la gestione delle schede GdR")
                        .version("v1.0")
                );
    }

}
