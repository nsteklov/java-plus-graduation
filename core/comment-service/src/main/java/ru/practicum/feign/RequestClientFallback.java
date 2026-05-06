package ru.practicum.feign;

public class RequestClientFallback implements RequestClient {

    @Override
    public boolean existsByRequesterIdAndEventIdAndStatus(Long userId, Long eventId, String status) {
        return false;
    }
}
