package ru.practicum.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ru.practicum.dto.SimilarityView;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.*;
import ru.practicum.model.Interaction;
import ru.practicum.model.Similarity;
import ru.practicum.repository.InteractionRepository;
import ru.practicum.repository.SimilarityRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class EventRecommendationsHandler {
    private final SimilarityRepository similarityRepository;
    private final InteractionRepository interactionRepository;
    public static final int SIMILAR_INTERACTED = 20;

    public EventRecommendationsHandler(SimilarityRepository similarityRepository, InteractionRepository interactionRepository) {
        this.similarityRepository = similarityRepository;
        this.interactionRepository = interactionRepository;
    }

    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto userPredictionsRequestProto) {

        Long maxResults = userPredictionsRequestProto.getMaxResults();
        Long userId = userPredictionsRequestProto.getUserId();

        Pageable pageable1 = PageRequest.of(0, Math.toIntExact(maxResults), Sort.by("ts").descending());
        List<Long> recentlyInteracted = interactionRepository.getByUserId(userId, pageable1).toList().stream()
                .map(Interaction::getEventId)
                .collect(Collectors.toList());
        if (recentlyInteracted.isEmpty()) {
            log.info("Пользователь с ид {} не взаимодействовал ни с какими мероприятиями", userId);
            return new ArrayList<>();
        }
        log.info("Получили {} мероприятий, с которыми пользователь с ид {} недавно взаимодействовал: {}", Math.toIntExact(maxResults), userId, recentlyInteracted);

        List<Similarity> similarEvents = similarityRepository.findByEventIds(recentlyInteracted);
        List<Long> eventsIds1 = similarEvents.stream()
                .map(Similarity::getEvent1)
                .collect(Collectors.toList());
        List<Long> eventsIds1Interactions = interactionRepository.findByUserIdAndEventIds(userId, eventsIds1);
        List<Long> eventsIds2 = similarEvents.stream()
                .map(Similarity::getEvent2)
                .collect(Collectors.toList());
        List<Long> eventsIds2Interactions = interactionRepository.findByUserIdAndEventIds(userId, eventsIds2);
        List<Similarity> similarEventsNotInteracted = new ArrayList<>();
        for (Similarity similarity : similarEvents) {
            if (eventsIds1Interactions.contains(similarity.getEvent1()) && eventsIds2Interactions.contains(similarity.getEvent2())) {
                continue;
            }
            similarEventsNotInteracted.add(similarity);
        }
        List<Similarity> recommendedEventsSorted = similarEventsNotInteracted.stream()
                .sorted(Comparator.comparingDouble(Similarity::getSimilarity).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
        log.info("Получили похожие новые мероприятия для пользователя с ид {}: {}", userId, recommendedEventsSorted);

        List<Long> recommendedEventIds1 = recommendedEventsSorted.stream()
                .map(Similarity::getEvent1)
                .collect(Collectors.toList());
        List<Long> recommendedEventIds2 = recommendedEventsSorted.stream()
                .map(Similarity::getEvent2)
                .collect(Collectors.toList());
        List<Long> recommendedEventIds = new ArrayList<>();
        recommendedEventIds.addAll(recommendedEventIds1);
        recommendedEventIds.addAll(recommendedEventIds2);
        List<RecommendedEventProto> recommendedEventsProto = new ArrayList<>();
        for (Long recommendedEventId : recommendedEventIds) {
            Pageable pageable2 = PageRequest.of(0, SIMILAR_INTERACTED);
            List<SimilarityView> similarInteractions = similarityRepository.findSimilar(recommendedEventId, userId, pageable2).toList();
            log.info("Получили {} коэффициенты подобия и оценки ближайших соседей к событию с ид {}: {}", SIMILAR_INTERACTED, recommendedEventId, similarInteractions);
            double weightedRatings = similarInteractions.stream()
                    .mapToDouble(similarityView -> similarityView.getSimilarity() * similarityView .getRating())
                    .sum();
            log.info("Получена сумма взвешенных оценок для ближайиших соседей события с ид {}: {}", recommendedEventId, weightedRatings);
            double similaritiesSum = similarInteractions.stream()
                    .mapToDouble(SimilarityView::getSimilarity)
                    .sum();
            log.info("Получена сумма коэффициентов подобия для ближайиших соседей события с ид {}: {}", recommendedEventId, similaritiesSum);
            double newRating = weightedRatings / similaritiesSum;
            log.info("Получена предсказанная оценка события с ид {}: {}", recommendedEventId, newRating);
            RecommendedEventProto recommendedEventProto = RecommendedEventProto.newBuilder()
                    .setEventId(recommendedEventId)
                    .setScore(newRating)
                    .build();
            recommendedEventsProto.add(recommendedEventProto);
        }

        log.info("Возвращены предсказанные оценки событий для пользователя с ид {}: {}", userId, recommendedEventsProto);
        return recommendedEventsProto;
    }


    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto similarEventsRequestProto) {

        Long maxResults = similarEventsRequestProto.getMaxResults();
        Long eventId = similarEventsRequestProto.getEventId();
        Long userId = similarEventsRequestProto.getUserId();

        List<Similarity> similarEvents = similarityRepository.findByEventId(eventId);
        log.info("Получили мероприятия похожие на мероприятие с id {}: {}", eventId, similarEvents);

        List<Long> eventsIds1 = similarEvents.stream()
                .map(Similarity::getEvent1)
                .collect(Collectors.toList());
        List<Long> eventsIds1Interactions = interactionRepository.findByUserIdAndEventIds(userId, eventsIds1);
        List<Long> eventsIds2 = similarEvents.stream()
                .map(Similarity::getEvent2)
                .collect(Collectors.toList());
        List<Long> eventsIds2Interactions = interactionRepository.findByUserIdAndEventIds(userId, eventsIds2);

        List<RecommendedEventProto> recommendedEventsProto = new ArrayList<>();
        for (Similarity similarity : similarEvents) {
            if (eventsIds1Interactions.contains(similarity.getEvent1()) && eventsIds2Interactions.contains(similarity.getEvent2())) {
                continue;
            }
            Long eventIdRecommended;
            if (eventId.equals(similarity.getEvent2())) {
                eventIdRecommended = similarity.getEvent1();
            } else {
                eventIdRecommended = similarity.getEvent2();
            }
            RecommendedEventProto recommendedEventProto = RecommendedEventProto.newBuilder()
                    .setEventId(eventIdRecommended)
                    .setScore(similarity.getSimilarity())
                    .build();
            recommendedEventsProto.add(recommendedEventProto);
        }
        List<RecommendedEventProto> recommendedEventsProtoSorted = recommendedEventsProto.stream()
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
        log.info("Отобрали мероприятия похожие на мероприятие с ид {}, исключив те, где пользователь взаимодействовал с обоими мероприятиями: {}", eventId, recommendedEventsProtoSorted);

        return recommendedEventsProtoSorted;
    }
}
