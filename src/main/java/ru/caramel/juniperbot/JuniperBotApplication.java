package ru.caramel.juniperbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;

@SpringBootConfiguration
@ImportResource("classpath:spring-context/app-context.xml")
public class JuniperBotApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(JuniperBotApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(JuniperBotApplication.class);
    }
}
