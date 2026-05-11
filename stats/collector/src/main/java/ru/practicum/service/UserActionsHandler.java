package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
public class UserActionsHandler {
    private final KafkaProducerService kafkaProducerService;

    public UserActionsHandler(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    public void handle(UserActionProto userActionProto)  {

        log.info("обработка сообщения: {}", userActionProto.getAllFields());

        ActionTypeAvro actionTypeAvro = null;
        switch(userActionProto.getActionType()){
            case ActionTypeProto.ACTION_LIKE:
                actionTypeAvro = ActionTypeAvro.LIKE;
                break;
            case ActionTypeProto.ACTION_REGISTER:
                actionTypeAvro = ActionTypeAvro.REGISTER;
                break;
            case ActionTypeProto.ACTION_VIEW:
                actionTypeAvro = ActionTypeAvro.VIEW;
                break;
        }
        if(actionTypeAvro == null) {
            throw new IllegalArgumentException("Не найден тип действия пользователя в avro - схеме для типа события: " + userActionProto.getActionType());
        }

        Instant timestamp = Instant.ofEpochSecond(
                userActionProto.getTimestamp().getSeconds(),
                userActionProto.getTimestamp().getNanos()
        );

        UserActionAvro userActionAvro = UserActionAvro.newBuilder()
                .setUserId(userActionProto.getUserId())
                .setEventId(userActionProto.getEventId())
                .setActionType(actionTypeAvro)
                .setTimestamp(timestamp)
                .build();

        log.info("попытка отправки сообщения: {}", userActionAvro.toString());
        kafkaProducerService.send(userActionAvro, userActionAvro.getUserId(), timestamp);
    }
}
