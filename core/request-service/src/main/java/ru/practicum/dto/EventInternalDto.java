package ru.practicum.dto;

import lombok.Data;

@Data
public class EventInternalDto {

    private Long eventId;
    private Long initiatorId;
    private Integer participantLimit;
    private boolean requestModeration;
    private String state;
}
