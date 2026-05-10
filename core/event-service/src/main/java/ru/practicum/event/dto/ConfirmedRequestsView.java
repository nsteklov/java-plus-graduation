package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfirmedRequestsView {

    private Long eventId;
    private Long quantity;
}
