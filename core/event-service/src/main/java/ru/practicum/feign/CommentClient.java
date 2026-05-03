package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "comment-service")
public interface CommentClient {

    @GetMapping("/internal/comments")
    Long countByEventIdAndStatus(@RequestParam Long eventId, @RequestParam String status);
}
