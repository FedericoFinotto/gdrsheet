package it.fin8.gdrsheet.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.boot.jdbc.DataSourceBuilder;

@Configuration
@PropertySource(
        value = {
                "file:C:/opt/gdrsheet/config/database.properties",
                "file:/opt/gdrsheet/config/database.properties"
        },
        ignoreResourceNotFound = true,   // se è false, fallisce subito se il file non esiste
        encoding = "UTF-8"
)
public class DatabaseConfig {

    @Autowired
    private Environment env;

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName(env.getProperty("spring.datasource.driver-class-name"))
                .url(env.getProperty("spring.datasource.url"))
                .username(env.getProperty("spring.datasource.username"))
                .password(env.getProperty("spring.datasource.password"))
                .build();
    }
}
