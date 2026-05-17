package ru.practicum.configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("collector")
@Getter
@Setter
public class KafkaPropertiesConfig {
    private String bootstrapServers;
    private String userActionsTopic;
    private Producer producer = new Producer();

    @Data
    public static class Producer {
        private String keySerializer;
        private String valueSerializer;
    }
}
