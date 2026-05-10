package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.event.dto.UserShortDto;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/internal/users/check")
    boolean userExists(@RequestParam Long userId);

    @GetMapping("/internal/users/{userId}")
    UserShortDto getUserDto(@PathVariable Long userId);
}
