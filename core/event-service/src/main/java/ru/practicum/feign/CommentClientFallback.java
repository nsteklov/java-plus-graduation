package ru.practicum.feign;

public class CommentClientFallback implements CommentClient {

    @Override
    public Long countByEventIdAndStatus(Long eventId, String status) {
        return 0L;
    }
}
