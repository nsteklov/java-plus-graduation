package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@Slf4j
public class Analyzer {
    public static void main(String[] args) {
        SpringApplication.run(Analyzer.class, args);
    }
}
