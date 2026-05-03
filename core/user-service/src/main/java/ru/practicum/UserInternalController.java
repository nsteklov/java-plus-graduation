package ru.practicum;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;
import ru.practicum.dto.UserShortDto;

@RestController
@RequestMapping(path = "/internal/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserInternalController {

    private final UserService userService;

    @GetMapping("/check")
    @ResponseStatus(HttpStatus.OK)
    public boolean userExists(@RequestParam Long userId) {
        log.info("GET запрос на проверку существования пользователя с id: {}", userId);
        return userService.userExists(userId);
    }

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserShortDto getUserDto(@PathVariable Long userId) {
        log.info("GET запрос на получение пользователя с id: {}", userId);
        return userService.getUserDto(userId);
    }
}
