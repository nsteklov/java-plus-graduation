package ru.practicum.exception;

import lombok.Data;

@Data
public class ErrorResponse {

    String reason;
    String message;
    String timestamp;
}
