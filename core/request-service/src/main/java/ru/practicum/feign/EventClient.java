package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.EventInternalDto;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient {

    @GetMapping("/internal/events/check")
    boolean eventExists(@RequestParam Long eventId);

    @GetMapping("/internal/events/{eventId}")
    EventInternalDto getEventDto(@PathVariable Long eventId);
}
