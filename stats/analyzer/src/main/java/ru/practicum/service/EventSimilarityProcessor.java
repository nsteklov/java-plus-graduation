package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.configuration.KafkaPropertiesConfigAnalyzer;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.repository.SimilarityRepository;
import ru.practicum.model.Similarity;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class EventSimilarityProcessor {
    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final KafkaPropertiesConfigAnalyzer propertiesConfig;
    private Consumer<String, EventSimilarityAvro> consumer;
    private String eventsSimilarityTopic;
    private final SimilarityRepository similarityRepository;

    public EventSimilarityProcessor(KafkaPropertiesConfigAnalyzer propertiesConfig, SimilarityRepository similarityRepository) {
        this.propertiesConfig = propertiesConfig;
        this.similarityRepository = similarityRepository;

        Properties consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, propertiesConfig.getClientIdSimilarities());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, propertiesConfig.getGroupIdSimilarities());
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, propertiesConfig.getBootstrapServers());
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, propertiesConfig.getEventSimilarityKeyDeserializer());
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, propertiesConfig.getEventSimilarityValueDeserializer());
        consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, propertiesConfig.getMaxPollRecordsConfig());
        consumerConfig.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, propertiesConfig.getFetchMaxBytesConfig());
        consumerConfig.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, propertiesConfig.getMaxPartitionFetchBytesConfig());
        consumer = new KafkaConsumer<>(consumerConfig);

        eventsSimilarityTopic = propertiesConfig.getEventsSimilarityTopic();
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            // подписываемся на топики
            List<String> topicsConsumer = new ArrayList<>();
            topicsConsumer.add(eventsSimilarityTopic);
            consumer.subscribe(topicsConsumer);

            // начинаем Poll Loop
            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                int count = 0;
                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    // обрабатываем очередную запись
                    try {
                        handleRecord(record);
                    } catch (Exception e) {
                        log.error("Возникла ошибка при обработке сообщения", e);
                    }
                    // фиксируем оффсеты обработанных записей, если нужно
                    manageOffsets(record, count, consumer);
                    count++;
                }
                // фиксируем максимальный оффсет обработанных записей
                consumer.commitAsync();
            }
        } catch (WakeupException ignores) {
            // Ничего здесь не делаем.
            // Закрываем консьюмер в finally блоке.
        } finally {
            // Перед закрытием консьюмера убеждаемся, что оффсеты обработанных сообщений
            // точно зафиксированы, вызываем для этого метод синхронной фиксации
            try {
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
            }
        }
    }

    private static void manageOffsets(ConsumerRecord<String, EventSimilarityAvro> record, int count, Consumer<String, EventSimilarityAvro> consumer) {
        // обновляем текущий оффсет для топика-партиции
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private void handleRecord(ConsumerRecord<String, EventSimilarityAvro> record) throws InterruptedException {
        log.info("Принимаем сообщение о сходстве событий, топик = {}, партиция = {}, смещение = {}, значение: {}\n",
                record.topic(), record.partition(), record.offset(), record.value());
        EventSimilarityAvro eventSimilarityAvro = record.value();
        Long eventA = eventSimilarityAvro.getEventA();
        Long eventB = eventSimilarityAvro.getEventB();
        Similarity similarity;
        Optional<Similarity> optSimilarity = similarityRepository.findByEvent1AndEvent2(eventA, eventB);
        if (optSimilarity.isPresent()) {
            similarity = optSimilarity.get();
            log.info("Получили имеющийся коэффициент сходства событий с ид {} и {}: {}", eventA, eventB, similarity.getSimilarity());
            if (similarity.getSimilarity() != eventSimilarityAvro.getScore()) {
                similarity.setSimilarity(eventSimilarityAvro.getScore());
                similarity.setTs(eventSimilarityAvro.getTimestamp());
                similarityRepository.save(similarity);
                log.info("Обновили коэффициент сходства событий с ид {} и {}: {}", eventA, eventB, eventSimilarityAvro.getScore());
            } else {
                log.info("Обновлять коэффициент сходства не требуется");
            }
        } else {
            similarity = new Similarity();
            similarity.setEvent1(eventA);
            similarity.setEvent2(eventB);
            similarity.setSimilarity(eventSimilarityAvro.getScore());
            similarity.setTs(eventSimilarityAvro.getTimestamp());
            similarityRepository.save(similarity);
            log.info("Записали новый коэффициент сходства событий с ид {} и {}: {}", eventA, eventB, eventSimilarityAvro.getScore());
        }
    }
}
