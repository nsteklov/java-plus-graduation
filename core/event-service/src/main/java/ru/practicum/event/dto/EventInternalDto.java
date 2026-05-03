package ru.practicum.event.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventInternalDto {

    private Long eventId;
    private Long initiatorId;
    private Integer participantLimit;
    private boolean requestModeration;
    private String state;
}
