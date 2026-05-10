package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.ConfirmedRequestsView;
import ru.practicum.service.RequestEventService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
@Slf4j
public class RequestInternalController {

    private final RequestEventService requestEventService;

    @PostMapping("/count")
    public List<ConfirmedRequestsView> countConfirmedRequestsByEventIds(@RequestBody Set<Long> eventIds) {
        log.info("GET запрос на получение подтвержденных заявок на участие по событиям с ид ", eventIds);
        return requestEventService.countConfirmedRequestsByEventIds(eventIds);
    }

    @GetMapping("/{userId}/{eventId}/check")
    boolean existsByRequesterIdAndEventIdAndStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestParam String status) {
        log.info("GET запрос на проверку наличия заявок на участие с ИД пользователя {}, ИД события {} и статусом {} ", userId, eventId, status);
        return requestEventService.existsByRequesterIdAndEventIdAndStatus(userId, eventId, status);
    }
}
