package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.EventInternalDto;
import ru.practicum.dto.NewCommentDto;

import ru.practicum.exception.ConditionsNotMetException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.EventClient;
import ru.practicum.feign.RequestClient;
import ru.practicum.feign.UserClient;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.CommentStatus;
import ru.practicum.repository.CommentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final RequestClient requestClient;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Создание комментария пользователем {} к событию {}", userId, eventId);

        if (!userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        if (eventId == null || !eventClient.eventExists(eventId)) {
            throw new NotFoundException("Событие с id = " + userId + " не найдено.");
        }

        EventInternalDto eventInternalDto = eventClient.getEventDto(eventId);
        if (eventInternalDto.getState() != "PUBLISHED") {
            throw new ConditionsNotMetException("Комментировать можно только опубликованные события");
        }

        boolean hasParticipated = requestClient.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, "CONFIRMED"
        );

        if (!hasParticipated) {
            throw new ConditionsNotMetException("Комментировать могут только участники события");
        }

        Comment comment = CommentMapper.toEntity(newCommentDto);
        comment.setAuthorId(userId);
        comment.setEventId(eventId);
        comment.setStatus(CommentStatus.PENDING); // Всегда на модерации

        Comment savedComment = commentRepository.save(comment);
        log.info("Создан комментарий с ID {}", savedComment.getId());

        return CommentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Удаление комментария {} пользователем {}", commentId, userId);

        Comment comment = commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        commentRepository.delete(comment);
    }

    @Override
    public List<CommentDto> getUserComments(Long userId, int from, int size) {
        log.info("Получение комментариев пользователя {}", userId);

        if (!userClient.userExists(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }

        Pageable pageable = PageRequest.of(from / size, size);

        return commentRepository.findByAuthorId(userId, pageable).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId, int from, int size) {
        log.info("Получение комментариев события {}", eventId);

        if (!eventClient.eventExists(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }

        Pageable pageable = PageRequest.of(from / size, size);

        return commentRepository.findByEventIdAndStatus(eventId, CommentStatus.PUBLISHED, pageable).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentsForModeration(int from, int size) {
        log.info("Получение комментариев на модерацию");

        Pageable pageable = PageRequest.of(from / size, size);

        return commentRepository.findByStatus(CommentStatus.PENDING, pageable).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto moderateComment(Long commentId, Boolean approve) {
        log.info("Модерация комментария {}", commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConditionsNotMetException("Можно модерировать только комментарии на рассмотрении");
        }

        comment.setStatus(approve ? CommentStatus.PUBLISHED : CommentStatus.REJECTED);
        Comment moderatedComment = commentRepository.save(comment);

        return CommentMapper.toDto(moderatedComment);
    }

    @Override
    public Long countByEventIdAndStatus(Long eventId, String stringStatus) {
        log.info("Подсчет количества комментариев с  ИД события {} и статусом {} ", eventId, stringStatus);
        CommentStatus status = CommentStatus.valueOf(stringStatus);
        return commentRepository.countByEventIdAndStatus(eventId,  status);
    }

}