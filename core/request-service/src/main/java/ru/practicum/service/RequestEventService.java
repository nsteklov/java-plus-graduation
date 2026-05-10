package ru.practicum.service;

import ru.practicum.dto.ConfirmedRequestsView;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Set;

public interface RequestEventService {

    /**
     * Получение информации о запросах на участие в событии текущего пользователя
     * @param userId
     * @param eventId
     * @return список объектов с типом ParticipationRequestDto
     */
    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId);

    /**
     * Изменение статуса заявок на участие в событии текущего пользователя
     * @param userId
     * @param eventId
     * @param updateRequest
     * @return объект с типом EventRequestStatusUpdateResult
     */
    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest updateRequest);


    /**
     * Получение подтвержденных заявок на участие по событиям
     * @param eventIds
     * @return список объектов с типом ConfirmedRequestsView
     */
    List<ConfirmedRequestsView> countConfirmedRequestsByEventIds(Set<Long> eventIds);

    /**
     * Проверка наличия заявок на участие
     * @param userId
     * @param eventId
     * @param status
     * @return Boolean
     */
    boolean existsByRequesterIdAndEventIdAndStatus(Long userId, Long eventId, String status);
}