package ru.practicum.exception;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {

    String reason;
    String message;
    String timestamp;
}
