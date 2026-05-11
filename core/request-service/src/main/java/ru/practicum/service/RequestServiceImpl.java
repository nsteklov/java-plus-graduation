package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EventInternalDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.dto.ParticipationRequestDto;
import ru.practicum.feign.EventClient;
import ru.practicum.feign.UserClient;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.RequestRepository;
import ru.practicum.StatsClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final StatsClient statsClient;

    /*
     * Добавление запроса на участие в событии
     */
    @Override
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        log.info("Пользователь userId = {} создает запрос на участие в событии eventId = {}", userId, eventId);

        // Проверка на добавление повторного запроса
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            log.warn("Попытка повторного запроса: userId = {}, eventId = {}", userId, eventId);
            throw new ConflictException("Нельзя добавить повторный запрос.");
        }

        // Проверка существования пользователя и события, валидация события
        if (userId == null || !userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }
        if (eventId == null || !eventClient.eventExists(eventId)) {
            throw new NotFoundException("Событие с id = " + userId + " не найдено.");
        }
        EventInternalDto eventDto = eventClient.getEventDto(eventId);
        validateEventForRequest(eventDto, userId);

        Request request = Request.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .status(RequestStatus.PENDING)
                .build();

        if (!eventDto.isRequestModeration() || eventDto.getParticipantLimit() == 0) {
            log.info("Заявка подтверждена автоматически (лимит = 0 или модерация отключена): eventId = {}", eventId);
            request.setStatus(RequestStatus.CONFIRMED);
        }

        Request savedRequest = requestRepository.save(request);
        statsClient.hit(userId, eventId, "REGISTER", LocalDateTime.now());
        log.info("Запрос успешно создан: requestId = {}, status = {}", savedRequest.getId(), savedRequest.getStatus());

        return RequestMapper.toDto(savedRequest);
    }

    /*
     * Отмена запроса на участие в событии
     */
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Пользователь userId = {} отменяет запрос requestId = {}", userId, requestId);

        // Проверка существования пользователя
        if (userId == null || !userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }

        // Проверка и получение запроса
        Request request = findRequestAndCheckOwner(requestId, userId);
        request.setStatus(RequestStatus.CANCELED);

        Request savedRequest = requestRepository.save(request);
        log.info("Запрос requestId = {} переведен в статус CANCELED", requestId);

        return RequestMapper.toDto(savedRequest);
    }

    /*
     * Получение списка запросов текущего пользователя на участие в чужих событиях
     */
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение всех запросов пользователя userId = {}", userId);

        // Проверка существования пользователя
        if (userId == null || !userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден.");
        }

        List<ParticipationRequestDto> requests = requestRepository.findAllByRequesterId(userId).stream()
                .map(RequestMapper::toDto)
                .collect(Collectors.toList());

        log.info("Найдено {} запросов для userId = {}", requests.size(), userId);

        return requests;
    }

    /*
     * Валидация события для запроса
     */
    private void validateEventForRequest(EventInternalDto eventDto, Long userId) {
        if (eventDto.getInitiatorId().equals(userId)) {
            throw new ConflictException("Инициатор события не может добавить запрос на участие в своём событии.");
        }

        if (!eventDto.getState().equals("PUBLISHED")) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии.");
        }

        if (eventDto.getParticipantLimit() > 0) {
            Long confirmedRequests = requestRepository
                    .countByEventIdAndStatus(eventDto.getEventId(), RequestStatus.CONFIRMED);

            if (confirmedRequests >= eventDto.getParticipantLimit()) {
                throw new ConflictException("У события достигнут лимит запросов на участие.");
            }
        }
    }

    /*
     * Проверка существования запроса и проверка запроса пользователя
     */
    private Request findRequestAndCheckOwner(Long requestId, Long userId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id = " + requestId + " не найден."));

        if (!Objects.equals(request.getRequesterId(), userId)) {
            throw new NotFoundException("Запрос с id = " + requestId + " не найден у текущего пользователя.");
        }
        return request;
    }
}
