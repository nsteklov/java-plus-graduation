package ru.practicum.configuration;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("aggregator")
@Getter
@Setter
public class KafkaPropertiesConfigAggregator {
    private String bootstrapServers;
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    private Topics topics = new Topics();

    @Data
    public static class Producer {
        private String keySerializer;
        private String valueSerializer;
    }

    @Data
    public static class Consumer {
        private String clientId;
        private String groupId;
        private String keyDeserializer;
        private String valueDeserializer;
        private int maxPollRecordsConfig;
        private int fetchMaxBytesConfig;
        private int maxPartitionFetchBytesConfig;
    }

    @Data
    public static class Topics {
        private String userActionsTopic;
        private String eventsSimilarityTopic;
    }
}