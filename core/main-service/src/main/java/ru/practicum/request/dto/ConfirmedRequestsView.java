package ru.practicum.request.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConfirmedRequestsView {

    private Long id;
    private Long quantity;
}
