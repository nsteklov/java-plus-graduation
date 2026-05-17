package ru.practicum.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.service.UserActionsHandler;

@GrpcService
@Slf4j
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final UserActionsHandler userActionsHandler;

    public UserActionController(UserActionsHandler userActionsHandler) {
        this.userActionsHandler = userActionsHandler;
    }

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            // передаём событие на обработку
            log.info("Событие передано в обработку {}", request.getAllFields());
            userActionsHandler.handle(request);
            // после обработки события возвращаем ответ клиенту
            responseObserver.onNext(Empty.getDefaultInstance());
            // и завершаем обработку запроса
            responseObserver.onCompleted();
        } catch (Exception e) {
            // в случае исключения отправляем ошибку клиенту
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }
}
