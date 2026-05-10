package ru.practicum.feign;

import ru.practicum.dto.EventInternalDto;

public class EventClientFallback implements EventClient {

    @Override
    public boolean eventExists(Long eventId) {
        return false;
    }

    @Override
    public EventInternalDto getEventDto(Long eventId) {
        return null;
    }
}
