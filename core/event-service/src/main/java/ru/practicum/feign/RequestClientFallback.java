package ru.practicum.feign;

import ru.practicum.event.dto.ConfirmedRequestsView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RequestClientFallback implements RequestClient {

    @Override
    public List<ConfirmedRequestsView> countConfirmedRequestsByEventIds(Set<Long> eventIds) {
        return new ArrayList<>();
    }

    @Override
    public boolean existsByRequesterIdAndEventIdAndStatus(Long userId, Long eventId, String status) {
        return false;
    }
}
