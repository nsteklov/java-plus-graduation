package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.EventClient;
import ru.practicum.feign.UserClient;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.RequestRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestEventServiceImpl implements RequestEventService {

    private final RequestRepository requestRepository;
    private final EventClient eventClient;
    private final UserClient userClient;

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        log.info("Получение запросов на участие в событии eventId = {} пользователем userId = {}", eventId, userId);

        // Проверка существования пользователя
        if (userId == null || !userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }

        // Проверка существования события и что пользователь является инициатором
        if (eventId == null || !eventClient.eventExists(eventId)) {
            throw new NotFoundException("Событие с id = " + userId + " не найдено.");
        }
        EventInternalDto eventDto = eventClient.getEventDto(eventId);
        if (!eventDto.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено у текущего пользователя.");
        }

        List<ParticipationRequestDto> requests = requestRepository.findAllByEventId(eventId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());

        log.info("Найдено {} запросов для события eventId = {}", requests.size(), eventId);
        return requests;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest updateRequest) {
        log.info("Изменение статуса запросов для события eventId = {} пользователем userId = {}", eventId, userId);

        // Проверка существования пользователя
        if (userId == null || !userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }

        // Проверка существования события и что пользователь является инициатором
        if (eventId == null || !eventClient.eventExists(eventId)) {
            throw new NotFoundException("Событие с id = " + userId + " не найдено.");
        }
        EventInternalDto eventDto = eventClient.getEventDto(eventId);
        if (!eventDto.getInitiatorId().equals(userId)) {
            throw new NotFoundException("Событие с id = " + eventId + " не найдено у текущего пользователя.");
        }

        // Проверка лимита участников
        if (eventDto.getParticipantLimit() > 0) {
            Long confirmedRequests = requestRepository
                    .countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (confirmedRequests >= eventDto.getParticipantLimit()) {
                throw new ConflictException("Достигнут лимит одобренных заявок");
            }
        }

        List<Request> requestsToUpdate = requestRepository.findAllByIdIn(updateRequest.getRequestIds());

        // Проверка, что все запросы относятся к данному событию
        for (Request request : requestsToUpdate) {
            if (!request.getEventId().equals(eventId)) {
                throw new NotFoundException("Запрос с id = " + request.getId() + " не относится к событию " + eventId);
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        RequestStatus status = updateRequest.getStatus();
        Long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

        for (Request request : requestsToUpdate) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Статус можно изменить только у заявок, находящихся в состоянии ожидания");
            }

            if (status == RequestStatus.CONFIRMED) {
                if (eventDto.getParticipantLimit() == 0 || !eventDto.isRequestModeration()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(RequestMapper.toDto(request));
                } else if (eventDto.getParticipantLimit() > 0 && confirmedRequests < eventDto.getParticipantLimit()) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(RequestMapper.toDto(request));
                    confirmedRequests++;
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejected.add(RequestMapper.toDto(request));
                }
            } else if (status == RequestStatus.REJECTED) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(RequestMapper.toDto(request));
            }

            requestRepository.save(request);
        }

        // Если при подтверждении лимит исчерпан, отклоняем все оставшиеся
        if (status == RequestStatus.CONFIRMED && eventDto.getParticipantLimit() > 0 &&
                confirmedRequests >= eventDto.getParticipantLimit()) {
            List<Request> pendingRequests = requestRepository.findAllByEventId(eventId).stream()
                    .filter(r -> r.getStatus() == RequestStatus.PENDING)
                    .collect(Collectors.toList());

            for (Request request : pendingRequests) {
                request.setStatus(RequestStatus.REJECTED);
                requestRepository.save(request);
                rejected.add(RequestMapper.toDto(request));
            }
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmed);
        result.setRejectedRequests(rejected);

        return result;
    }

    @Override
    public List<ConfirmedRequestsView> countConfirmedRequestsByEventIds(Set<Long> eventIds) {
        log.info("Получение подтвержденных заявок на участие по событиям с ид ", eventIds);
        return requestRepository.countConfirmedRequestsByEventIds(eventIds);
    }

    @Override
    public boolean existsByRequesterIdAndEventIdAndStatus(Long userId, Long eventId, String stringStatus) {
        log.info("Проверка наличия заявок на участие с ИД пользователя {}, ИД события {} и статусом {} ", userId, eventId, stringStatus);
        RequestStatus status = RequestStatus.valueOf(stringStatus);
        return requestRepository.existsByRequesterIdAndEventIdAndStatus(userId, eventId,  status);
    }
}