package ru.practicum.request.service;

import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    /**
     * Добавление запроса на участие в событии
     * @param userId
     * @param eventId
     * @return объект с типом ParticipationRequestDto
     */
    ParticipationRequestDto addRequest(Long userId, Long eventId);

    /**
     * Отмена запроса на участие в событии
     * @param userId
     * @param requestId
     * @return объект с типом ParticipationRequestDto
     */
    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    /**
     * Получение списка запросов текущего пользователя на участие в чужих событиях
     * @param userId
     * @return список объектов с типом ParticipationRequestDto
     */
    List<ParticipationRequestDto> getUserRequests(Long userId);
}
