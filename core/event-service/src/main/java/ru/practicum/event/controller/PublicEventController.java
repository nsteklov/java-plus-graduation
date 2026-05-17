package ru.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.AnalyzerClient;
import ru.practicum.event.EventService;
import ru.practicum.event.SortEvents;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.params.PublicEventsParam;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicEventController {
    private final EventService eventService;
    private final AnalyzerClient analyzerClient;
    public static final int MAX_RESULTS = 20;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam (required = false) Long[] categories,
            @RequestParam (required = false) Boolean paid,
            @RequestParam (required = false) String rangeStart,
            @RequestParam (required = false) String rangeEnd,
            @RequestParam (defaultValue = "false") Boolean onlyAvailable,
            @RequestParam (defaultValue = "EVENT_DATE") SortEvents sort,
            @RequestParam (defaultValue = "0") Integer from,
            @RequestParam (defaultValue = "10") Integer size,
            HttpServletRequest request) {

        log.info("Получение событий через публичный эндпоинт");
        PublicEventsParam publicEventsParam = new PublicEventsParam(text, categories, paid,  rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        return eventService.getEventsPublic(publicEventsParam, request.getRemoteAddr(), request.getRequestURI());
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto findById(@PathVariable Long eventId, @RequestHeader("X-EWM-USER-ID") Long userId, HttpServletRequest request) {
        log.info("Получение полной информации о событии");
        return eventService.findById(eventId, request.getRemoteAddr(), request.getRequestURI(), userId);
    }

    @GetMapping("/recommendations")
    @ResponseStatus(HttpStatus.OK)
    public Map<Long, Double> recommendedEvents(@RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("Получение рекоммендаций для пользователя с ид {}", userId);
        return analyzerClient.getRecommendedEventsForUser(userId, MAX_RESULTS);
    }

    @PatchMapping("/{eventId}/like")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto addLike(@PathVariable Long eventId, @RequestHeader("X-EWM-USER-ID") Long userId) {
        log.info("Проставление лайка событию");
        return eventService.addLike(eventId, userId);
    }
}
