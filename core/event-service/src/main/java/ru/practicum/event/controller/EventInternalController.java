package ru.practicum.event.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.EventService;
import ru.practicum.event.dto.EventInternalDto;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EventInternalController {

    private final EventService eventService;

    @GetMapping("/check")
    @ResponseStatus(HttpStatus.OK)
    public boolean eventExists(@RequestParam Long eventId) {
        log.info("GET запрос на проверку существования события с id: {}", eventId);
        return eventService.eventExists(eventId);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventInternalDto getEventDto(@PathVariable Long eventId) {
        log.info("GET запрос на получение события с id: {}", eventId);
        return eventService.getEventDto(eventId);
    }
}
