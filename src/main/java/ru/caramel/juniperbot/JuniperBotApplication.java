package ru.caramel.juniperbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ImportResource;

@SpringBootConfiguration
@ImportResource("classpath:spring-context/app-context.xml")
public class JuniperBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(JuniperBotApplication.class, args);
    }
}
