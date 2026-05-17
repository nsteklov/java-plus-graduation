package ru.practicum.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserShortDto {

    @NotNull(message = "ID пользователя не может быть null")
    private Long id;

    @NotBlank(message = "Имя пользователя не может быть пустым")
    private String name;
}