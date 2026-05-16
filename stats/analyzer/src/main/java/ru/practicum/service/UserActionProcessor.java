package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.configuration.KafkaPropertiesConfigAnalyzer;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.Interaction;
import ru.practicum.repository.InteractionRepository;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class UserActionProcessor implements Runnable {
    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final KafkaPropertiesConfigAnalyzer propertiesConfig;
    private Consumer<Long, UserActionAvro> consumer;
    private String userActionsTopic;
    private final InteractionRepository interactionRepository;

    public UserActionProcessor(KafkaPropertiesConfigAnalyzer propertiesConfig, InteractionRepository interactionRepository) {
        this.propertiesConfig = propertiesConfig;
        this.interactionRepository = interactionRepository;

        Properties consumerConfig = new Properties();
        consumerConfig.put(ConsumerConfig.CLIENT_ID_CONFIG, propertiesConfig.getClientIdInteractions());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, propertiesConfig.getGroupIdInteractions());
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, propertiesConfig.getBootstrapServers());
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, propertiesConfig.getUserActionKeyDeserializer());
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, propertiesConfig.getUserActionValueDeserializer());
        consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, propertiesConfig.getMaxPollRecordsConfig());
        consumerConfig.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, propertiesConfig.getFetchMaxBytesConfig());
        consumerConfig.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, propertiesConfig.getMaxPartitionFetchBytesConfig());
        consumer = new KafkaConsumer<>(consumerConfig);

        userActionsTopic = propertiesConfig.getUserActionsTopic();
    }


    @Override
    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        try {
            // подписываемся на топики
            List<String> topicsConsumer = new ArrayList<>();
            topicsConsumer.add(userActionsTopic);
            consumer.subscribe(topicsConsumer);

            // начинаем Poll Loop
            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                int count = 0;
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
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

    private static void manageOffsets(ConsumerRecord<Long, UserActionAvro> record, int count, Consumer<Long, UserActionAvro> consumer) {
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

    private void handleRecord(ConsumerRecord<Long, UserActionAvro> record) throws InterruptedException {
        log.info("Принимаем сообщение о действии пользователя, топик = {}, партиция = {}, смещение = {}, значение: {}\n",
                record.topic(), record.partition(), record.offset(), record.value());
        UserActionAvro userActionAvro = record.value();
        Long userId = userActionAvro.getUserId();
        Long eventId = userActionAvro.getEventId();
        double weight = switch (userActionAvro.getActionType()) {
            case ru.practicum.ewm.stats.avro.ActionTypeAvro.VIEW -> 0.4;
            case ru.practicum.ewm.stats.avro.ActionTypeAvro.REGISTER -> 0.8;
            case ru.practicum.ewm.stats.avro.ActionTypeAvro.LIKE -> 1.0;
            default ->  0.0;
        };
        Interaction interaction;
        Optional<Interaction> optInteraction = interactionRepository.findByUserIdAndEventId(userId, eventId);
        if (optInteraction.isPresent()) {
            interaction = optInteraction.get();
            log.info("Получили прошлое взаимодействие пользователя с ид {} с событием с ид {}: {}", userId, eventId, interaction.getRating());
            if (interaction.getRating() < weight) {
                interaction.setRating(weight);
                interaction.setTs(userActionAvro.getTimestamp());
                interactionRepository.save(interaction);
                log.info("Обновили взаимодействие пользователя с ид {} с событием с ид {}: {}", userId, eventId, weight);
            } else {
                log.info("Обновлять взаимодействие не требуется");
            }
        } else {
            interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setEventId(eventId);
            interaction.setRating(weight);
            interaction.setTs(userActionAvro.getTimestamp());
            interactionRepository.save(interaction);
            log.info("Записали новое взаимодействие пользователя с ид {} с событием с ид {}: {}", userId, eventId, weight);
        }
    }
}
