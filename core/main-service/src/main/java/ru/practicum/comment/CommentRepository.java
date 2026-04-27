package ru.practicum.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Получить опубликованные комментарии события
     * Получить опубликованные комментарии события
     * @param eventId
     * @param status
     * @param pageable
     * @return список объектов с типом Comment
     */
    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    /**
     * Получить комментарии пользователя
     * @param authorId
     * @param pageable
     * @return список объектов с типом Comment
     */
    List<Comment> findByAuthorId(Long authorId, Pageable pageable);

    /**
     * Получить комментарии на модерации
     * @param status
     * @param pageable
     * @return список объектов с типом Comment
     */
    List<Comment> findByStatus(CommentStatus status, Pageable pageable);

    /**
     * Получить комментарий по ID и автору
     * @param id
     * @param authorId
     * @return Optional с объектом с типом Comment
     */
    Optional<Comment> findByIdAndAuthorId(Long id, Long authorId);


    /**
     * Подсчет опубликованных комментариев
     * @param eventId
     * @param status
     * @return Long
     */
    Long countByEventIdAndStatus(Long eventId, CommentStatus status);
}