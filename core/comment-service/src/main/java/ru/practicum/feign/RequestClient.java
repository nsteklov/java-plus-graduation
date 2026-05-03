package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/internal/requests/{userId}/{eventId}/check")
    boolean existsByRequesterIdAndEventIdAndStatus(@PathVariable Long userId, @PathVariable Long eventId, @RequestParam String status);
}
