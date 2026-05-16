package ru.practicum;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void hit(long userId, long eventId, String actionType, LocalDateTime timestamp) {

        Instant instant = timestamp.toInstant(ZoneOffset.UTC);

        ActionTypeProto actionTypeProto = null;
        switch(actionType){
            case "LIKE":
                actionTypeProto = ActionTypeProto.ACTION_LIKE;
                break;
            case "REGISTER":
                actionTypeProto = ActionTypeProto.ACTION_REGISTER;
                break;
            case "VIEW":
                actionTypeProto = ActionTypeProto.ACTION_VIEW;
                break;
        }

        UserActionProto userActionProto = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionTypeProto)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(instant.getEpochSecond())
                        .setNanos(instant.getNano()))
                .build();

        log.info("Отправка данных из клиента статистики: {}", userActionProto.getAllFields());
        collectorStub.collectUserAction(userActionProto);
    }
}