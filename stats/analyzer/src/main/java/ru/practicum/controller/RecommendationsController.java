package ru.practicum.controller;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.*;
import ru.practicum.service.EventRecommendationsHandler;

import java.util.Iterator;
import java.util.List;

@GrpcService
@Slf4j
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final EventRecommendationsHandler eventRecommendationsHandler;

    public RecommendationsController(EventRecommendationsHandler eventRecommendationsHandler) {
        this.eventRecommendationsHandler = eventRecommendationsHandler;
    }

//    @Override
//    public Iterator<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<Empty> responseObserver) {

    /// /        try {
    /// /            // передаём событие на обработку
    /// /            log.info("Событие передано в обработку {}", request.getAllFields());
    /// /            userActionsHandler.handle(request);
    /// /            // после обработки события возвращаем ответ клиенту
    /// /            responseObserver.onNext(Empty.getDefaultInstance());
    /// /            // и завершаем обработку запроса
    /// /            responseObserver.onCompleted();
    /// /        } catch (Exception e) {
    /// /            // в случае исключения отправляем ошибку клиенту
    /// /            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
    /// /        }
//        return RecommendedEventProto.newBuilder().build();
//    }
    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            // передаём событие на обработку
            log.info("Событие передано в обработку {}", request.getAllFields());
            List<RecommendedEventProto> recommendedEventsProto = eventRecommendationsHandler.getSimilarEvents(request);
            // после обработки события возвращаем ответ клиенту
            for (RecommendedEventProto recommendedEventProto : recommendedEventsProto) {
                // Отправка очередного объекта в поток
                responseObserver.onNext(recommendedEventProto);
                log.info("Объект {} отправлен в поток", recommendedEventProto.getAllFields());
            }
            // Сигнализируем о завершении потока
            responseObserver.onCompleted();
        } catch (Exception e) {
            // При ошибке отправляем исключение
            responseObserver.onError(e);
        }
    }
}
