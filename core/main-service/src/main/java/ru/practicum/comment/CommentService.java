package ru.practicum.comment;

import java.util.List;

public interface CommentService {

    /**
     * Private API (для пользователей)
     * Пользователь создает комментарий
     * @param userId
     * @param eventId
     * @param newCommentDto
     * @return объект с типом CommentDto
     */
    CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    /**
     * Пользователь удаляет свой комментарий
     * @param userId
     * @param commentId
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * Получить комментарии пользователя
     * @param userId
     * @param from
     * @param size
     * @return список объектов с типом CommentDto
     */
    List<CommentDto> getUserComments(Long userId, int from, int size);

    /**
     * Получить опубликованные комментарии события (публичный доступ)
     * @param eventId
     * @param from
     * @param size
     * @return список объектов с типом CommentDto
     */
    List<CommentDto> getEventComments(Long eventId, int from, int size);

    /**
     * Admin API (для администраторов)
     * Админ получает комментарии на модерации
     * @param from
     * @param size
     * @return список объектов с типом CommentDto
     */
    List<CommentDto> getCommentsForModeration(int from, int size);

    /**
     * Админ модерирует комментарий
     * @param commentId
     * @param approve
     * @return объект с типом CommentDto
     */
    CommentDto moderateComment(Long commentId, Boolean approve);
}