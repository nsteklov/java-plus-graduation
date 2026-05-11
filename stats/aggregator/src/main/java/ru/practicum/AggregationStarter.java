package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.configuration.KafkaPropertiesConfigAggregator;
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
    private Consumer<String, SensorEventAvro> consumer;
    private Producer<String, SensorsSnapshotAvro> producer;
    private Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();
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
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);
                int count = 0;
                for (ConsumerRecord<String, SensorEventAvro> record : records) {
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

    private static void manageOffsets(ConsumerRecord<String, SensorEventAvro> record, int count, Consumer<String, SensorEventAvro> consumer) {
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

    private void handleRecord(ConsumerRecord<String, SensorEventAvro> record) throws InterruptedException {
        log.info("Принимаем сообщение топик = {}, партиция = {}, смещение = {}, значение: {}\n",
                record.topic(), record.partition(), record.offset(), record.value());



        Optional<SensorsSnapshotAvro> sensorSnapshotAvroOptional = updateState(record.value());
        if (sensorSnapshotAvroOptional.isPresent()) {
            SensorsSnapshotAvro sensorSnapshotAvro = sensorSnapshotAvroOptional.get();
            log.info("Отправляем данные снапшота: {}", sensorSnapshotAvro.toString());
            ProducerRecord producerRecord = new ProducerRecord<>(snapshotTopic, null, sensorSnapshotAvro.getTimestamp().toEpochMilli(), sensorSnapshotAvro.getHubId(), sensorSnapshotAvro);
            Future<RecordMetadata> future = producer.send(producerRecord);
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
    }

    private Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        //Проверяем, есть ли снапшот для event.getHubId()
        //Если снапшот есть, то достаём его
        // Если нет, то создаём новый
        SensorsSnapshotAvro snapshot;
        if (!snapshots.containsKey(event.getHubId())) {
            snapshot = new SensorsSnapshotAvro();
            snapshot.setHubId(event.getHubId());
            snapshot.setSensorsState(new HashMap<>());
            log.info("Создаем новый снапшот с hub id = {}", snapshot.getHubId());
        } else {
            snapshot = snapshots.get(event.getHubId());
            log.info("Получаем имеющийся снапшот с hub id = {}", snapshot.getHubId());
        }

//        Проверяем, есть ли в снапшоте данные для event.getId()
//        Если данные есть, то достаём их в переменную oldState
//        Проверка, если oldState.getTimestamp() произошёл позже, чем
//        event.getTimestamp() или oldState.getData() равен
//        event.getPayload(), то ничего обнавлять не нужно, выходим из метода
//        вернув Optional.empty()

        if (snapshot.getSensorsState().containsKey(event.getId())) {
            SensorStateAvro oldState = snapshot.getSensorsState().get(event.getId());
            if (oldState.getTimestamp().isAfter(event.getTimestamp()) || oldState.getData().equals(event.getPayload())) {
                log.info("Снапшот обновлять не нужно");
                return Optional.empty();
            }
        }

        // если дошли до сюда, значит, пришли новые данные и
        // снапшот нужно обновить
        Instant timestamp = Instant.ofEpochSecond(
                event.getTimestamp().getEpochSecond(),
                event.getTimestamp().getNano());
        SensorStateAvro state = SensorStateAvro.newBuilder()
                .setTimestamp(timestamp)
                .setData(event.getPayload())
                .build();
        snapshot.setTimestamp(timestamp);
        snapshot.getSensorsState().put(event.getId(), state);
        snapshots.put(event.getHubId(), snapshot);
        return Optional.of(snapshot);
    }
}
