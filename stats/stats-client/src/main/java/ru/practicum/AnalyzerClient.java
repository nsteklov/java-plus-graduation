package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public Stream<RecommendedEventProto> getRecommendationsForUser(Long userId, int maxResults) {
        log.info("Получаем рекомендации для пользователя с ид {} maxResults: {}, в AnalyzerClient", userId, maxResults);
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEventProto> iterator = analyzerStub.getRecommendationsForUser(request);
        log.info("Получен поток рекомендаций {}", iterator);
        return asStream(iterator);
    }

    public Map<Long, Double> getRecommendedEventsForUser(Long userId, int maxResults) {
        Stream<RecommendedEventProto> recommendedEventsProtoStream = getRecommendationsForUser(userId, maxResults);
        Map<Long, Double> recommendedEvents = recommendedEventsProtoStream.toList().stream()
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore
                ));
        log.info("Получены рекомендации для пользователя с ид {}: {}", userId, recommendedEvents);
        return recommendedEvents;
    }

    public Stream<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, Integer maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        Iterator<RecommendedEventProto> iterator = analyzerStub.getSimilarEvents(request);

        return asStream(iterator);
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();

        Iterator<RecommendedEventProto> iterator = analyzerStub.getInteractionsCount(request);

        return asStream(iterator);
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}