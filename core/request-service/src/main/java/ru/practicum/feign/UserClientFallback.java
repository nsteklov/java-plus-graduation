package ru.practicum.feign;

public class UserClientFallback implements UserClient {

    @Override
    public boolean userExists(Long userId) {
        return false;
    }
}
