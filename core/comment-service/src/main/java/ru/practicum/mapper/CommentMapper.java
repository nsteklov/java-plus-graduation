package ru.practicum.mapper;

import ru.practicum.dto.CommentDto;
import ru.practicum.dto.NewCommentDto;
import ru.practicum.model.Comment;
import ru.practicum.model.CommentStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommentMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorId(comment.getAuthorId())
                .eventId(comment.getEventId())
                .created(comment.getCreated().format(FORMATTER))
                .status(comment.getStatus())
                .build();
    }

    public static Comment toEntity(NewCommentDto newCommentDto) {
        return Comment.builder()
                .text(newCommentDto.getText())
                .created(LocalDateTime.now())
                .status(CommentStatus.PENDING)
                .build();
    }
}