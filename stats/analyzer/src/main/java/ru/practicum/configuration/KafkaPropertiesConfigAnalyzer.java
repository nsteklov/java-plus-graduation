package ru.practicum.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("analyzer")
@Getter
@Setter
public class KafkaPropertiesConfigAnalyzer {
    private String bootstrapServers;
    private String clientIdInteractions;
    private String groupIdInteractions;
    private String clientIdSimilarities;
    private String groupIdSimilarities;
    private String userActionKeyDeserializer;
    private String eventSimilarityKeyDeserializer;
    private String userActionValueDeserializer;
    private String eventSimilarityValueDeserializer;
    private String userActionsTopic;
    private String eventsSimilarityTopic;
    private int maxPollRecordsConfig;
    private int fetchMaxBytesConfig;
    private int maxPartitionFetchBytesConfig;
}