package ru.practicum.feign;

import ru.practicum.event.dto.UserShortDto;

public class UserClientFallback implements UserClient {

    @Override
    public boolean userExists(Long userId) {
        return false;
    }

    @Override
    public UserShortDto getUserDto(Long userId) {
        return null;
    }
}
