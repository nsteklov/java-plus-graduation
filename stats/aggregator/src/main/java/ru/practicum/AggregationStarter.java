package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.practicum.configuration.KafkaPropertiesConfigAggregator;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class AggregationStarter {
    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final KafkaPropertiesConfigAggregator propertiesConfig;
    private Consumer<String, UserActionAvro> consumer;
    private Producer<String, UserActionAvro> producer;
    private Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();
    private Map<Long, Map<Long, Double>> userActionsWithEvents = new HashMap<>();
    private Map<Long, Double> totalWeightsSums = new HashMap<>();
    private Map<Long, Map<Long, Double>> minWeightsSums  = new HashMap<>();
    private List<Long> eventIds = new ArrayList<>();
    private List<Long> userIds = new ArrayList<>();
    private String userActionsTopic;
    private String eventsSimilarityTopic;

    public AggregationStarter(KafkaPropertiesConfigAggregator propertiesConfig) {
        this.propertiesConfig = propertiesConfig;
        Properties consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, propertiesConfig.getConsumer().getClientId());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, propertiesConfig.getConsumer().getGroupId());
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, propertiesConfig.getBootstrapServers());
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, propertiesConfig.getConsumer().getKeyDeserializer());
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, propertiesConfig.getConsumer().getValueDeserializer());
        consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, propertiesConfig.getConsumer().getMaxPollRecordsConfig());
        consumerConfig.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, propertiesConfig.getConsumer().getFetchMaxBytesConfig());
        consumerConfig.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, propertiesConfig.getConsumer().getMaxPartitionFetchBytesConfig());
        consumer = new KafkaConsumer<>(consumerConfig);

        Properties producerConfig = new Properties();
        producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, propertiesConfig.getBootstrapServers());
        producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, propertiesConfig.getProducer().getKeySerializer());
        producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, propertiesConfig.getProducer().getValueSerializer());
        producer = new KafkaProducer<>(producerConfig);

        userActionsTopic = propertiesConfig.getTopics().getUserActionsTopic();
        eventsSimilarityTopic = propertiesConfig.getTopics().getEventsSimilarityTopic();
    }

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {

            // подписываемся на топики
            List<String> topicsConsumer = new ArrayList<>();
            topicsConsumer.add(userActionsTopic);
            consumer.subscribe(topicsConsumer);

            // начинаем Poll Loop
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                int count = 0;
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    // обрабатываем очередную запись
                    handleRecord(record);
                    // фиксируем оффсеты обработанных записей, если нужно
                    manageOffsets(record, count, consumer);
                    count++;
                }
                // фиксируем максимальный оффсет обработанных записей
                consumer.commitAsync();
            }
        } catch (WakeupException | InterruptedException ignores) {
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
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    private static void manageOffsets(ConsumerRecord<String, UserActionAvro> record, int count, Consumer<String, UserActionAvro> consumer) {
        // обновляем текущий оффсет для топика-партиции
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if(count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if(exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private void handleRecord(ConsumerRecord<String, UserActionAvro> record) throws InterruptedException {
        log.info("Принимаем сообщение топик = {}, партиция = {}, смещение = {}, значение: {}\n",
                record.topic(), record.partition(), record.offset(), record.value());

        long userId = record.value().getUserId();
        long event1 = record.value().getEventId();
        ActionTypeAvro actionTypeAvro = record.value().getActionType();
        if (!eventIds.contains(event1)) {
            eventIds.add(event1);
        }
        double weight1New = switch (actionTypeAvro) {
            case ActionTypeAvro.VIEW -> 0.4;
            case ActionTypeAvro.REGISTER -> 0.8;
            case ActionTypeAvro.LIKE -> 1.0;
            default ->  0.0;
        };
        double weight1Old = userActionsWithEvents
                .computeIfAbsent(event1, e -> new HashMap<>())
                .getOrDefault(userId, 0.0);
        double totalWeightsSum1 = totalWeightsSums.getOrDefault(event1, 0.0);
        log.info("Оценка события с ид {} пользователем с ид {} было {}", event1, userId, weight1Old);
        log.info("Общая сумма оценок события с ид {} было {}", event1, totalWeightsSum1);
        if (weight1New <= weight1Old) {
            log.info("Оценка события с ид {} пользователем с ид {} не увеличилась, пересчет коэффициентов сходства не нужен", event1, userId);
            return;
        }
        userActionsWithEvents
                .computeIfAbsent(event1, e -> new HashMap<>())
                .put(userId, weight1New);
        totalWeightsSums.put(event1, totalWeightsSum1);
        totalWeightsSum1 = totalWeightsSum1 + weight1New - weight1Old;
        log.info("Оценка события с ид {} пользователем с ид {} стало {}", event1, userId, weight1New);
        log.info("Общая сумма оценок события с ид {} стало {}", event1, totalWeightsSum1);

        for (Long event2 : eventIds) {
            if (event1 == event2) {
                continue;
            }
            long first  = Math.min(event1, event2);
            long second = Math.max(event1, event2);
            double minWeightsSum = minWeightsSums
                    .computeIfAbsent(first, e -> new HashMap<>())
                    .getOrDefault(second, 0.0);
            double weight2 = userActionsWithEvents
                    .computeIfAbsent(event2, e -> new HashMap<>())
                    .getOrDefault(userId, 0.0);
            double totalWeightsSum2 = totalWeightsSums.getOrDefault(event2, 0.0);
            minWeightsSums
                    .computeIfAbsent(first, e -> new HashMap<>())
                    .put(second, minWeightsSum);
            log.info("Сумма минимальных весов событий с ид {} и {}  было {}", event1, event2, minWeightsSum);
            log.info("Оценка события с ид {} пользователем с ид {}: {}", event2, userId, weight2);
            log.info("Общая сумма оценок события с ид {}: {}", event2, totalWeightsSum2);
            if (!userIds.contains(userId)) {
                userActionsWithEvents
                        .computeIfAbsent(event2, e -> new HashMap<>())
                        .put(userId, weight2);
            }
            if (weight2 == 0 || totalWeightsSum2 == 0) {
                log.info("Рассчитывать коэффициент сходства событий {} и {} нет необходимости", event1, event2);
                return;
            }
            minWeightsSum = minWeightsSum + Math.min(weight1New, weight2) - Math.min(weight1Old, weight2);
            double similarity = minWeightsSum / (Math.sqrt(totalWeightsSum1) * Math.sqrt(totalWeightsSum2));
            log.info("Сумма минимальных весов событий с ид {} и {}  стало {}", event1, event2, minWeightsSum);
            log.info("Рассчитана величина сходства событий с ид {} и {}: {}", event1, event2, similarity);



//                SensorsSnapshotAvro sensorSnapshotAvro = sensorSnapshotAvroOptional.get();
//                log.info("Отправляем данные снапшота: {}", sensorSnapshotAvro.toString());
//                ProducerRecord producerRecord = new ProducerRecord<>(snapshotTopic, null, sensorSnapshotAvro.getTimestamp().toEpochMilli(), sensorSnapshotAvro.getHubId(), sensorSnapshotAvro);
//                Future<RecordMetadata> future = producer.send(producerRecord);
//                try {
//                    future.get(10, TimeUnit.SECONDS);
//                } catch (TimeoutException e) {
//                    // Превышено время ожидания
//                    log.error("Превышено время ожидания");
//                    future.cancel(true); // Отменяем задачу
//                } catch (InterruptedException | ExecutionException e) {
//                    log.error("Возникла ошибка при отправке сообщения: ", e);
//                }

        }
    }
}
