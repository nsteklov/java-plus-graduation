package ru.practicum;

public class StatsServerUnavailable extends RuntimeException {
    public StatsServerUnavailable(String message, Exception exception) {
        super(message, exception);
    }
}
