package ru.practicum;

import org.springframework.data.domain.Pageable;
import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;
import ru.practicum.dto.UserShortDto;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest newUserRequest);

    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    void deleteUser(Long userId);

    boolean userExists(Long userId);

    UserShortDto getUserDto(Long userId);
}