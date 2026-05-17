package ru.practicum.controller;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.*;
import ru.practicum.service.EventRecommendationsHandler;

import java.util.List;

@GrpcService
@Slf4j
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final EventRecommendationsHandler eventRecommendationsHandler;

    public RecommendationsController(EventRecommendationsHandler eventRecommendationsHandler) {
        this.eventRecommendationsHandler = eventRecommendationsHandler;
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            // передаём событие на обработку
            log.info("Событие передано в обработку {}", request.getAllFields());
            List<RecommendedEventProto> recommendedEventsProto = eventRecommendationsHandler.getRecommendationsForUser(request);
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

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            // передаём событие на обработку
            log.info("Событие передано в обработку {}", request.getAllFields());
            List<RecommendedEventProto> recommendedEventsProto = eventRecommendationsHandler.getInteractionsCount(request);
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
