package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.ConfirmedRequestsView;

import java.util.List;
import java.util.Set;

@FeignClient(name = "request-service", fallback = RequestClientFallback.class)
public interface RequestClient {

    @PostMapping("/internal/requests/count")
    List<ConfirmedRequestsView> countConfirmedRequestsByEventIds(@RequestBody Set<Long> eventIds);
}
