package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.*;
import org.springframework.stereotype.Service;
import ru.practicum.configuration.KafkaPropertiesConfig;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class KafkaProducerService implements AutoCloseable {

    private final KafkaPropertiesConfig propertiesConfig;
    private Producer<Long, SpecificRecordBase> producer;
    String userActionsTopic;

    public KafkaProducerService(KafkaPropertiesConfig propertiesConfig) {
        this.propertiesConfig = propertiesConfig;
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, propertiesConfig.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, propertiesConfig.getProducer().getKeySerializer());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, propertiesConfig.getProducer().getValueSerializer());
        userActionsTopic = propertiesConfig.getUserActionsTopic();
        producer = new KafkaProducer<>(config);
    }

    public void send(UserActionAvro value, long key, Instant timestamp) {
        ProducerRecord producerRecord = new ProducerRecord<>(userActionsTopic, null, timestamp.toEpochMilli(), key, value);
        Future<RecordMetadata> future = producer.send(producerRecord);
        log.info("отправлено сообщение: {}", value.toString());
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // Превышено время ожидания
            log.error("Превышено время ожидания");
            future.cancel(true); // Отменяем задачу
        } catch (InterruptedException | ExecutionException e) {
            log.error("Возникла ошибка при отправке сообщения: ", e);
        }
    }

    @Override
    public void close() {
        if (producer != null) {
            producer.flush();
            producer.close();
        }
    }
}
