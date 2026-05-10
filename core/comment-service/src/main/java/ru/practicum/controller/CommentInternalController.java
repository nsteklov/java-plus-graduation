package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.service.CommentService;

@RestController
@RequestMapping("/internal/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentInternalController {

    private final CommentService commentService;

    @GetMapping
    public Long countByEventIdAndStatus(@RequestParam Long eventId, @RequestParam String status) {
        log.info("GET /internal/comments?eventId={}&status={}", eventId, status);
        return commentService.countByEventIdAndStatus(eventId, status);
    }
}
