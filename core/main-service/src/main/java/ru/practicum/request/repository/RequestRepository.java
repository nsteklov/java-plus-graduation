package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.request.dto.ConfirmedRequestsView;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;

import java.util.List;
import java.util.Set;

public interface RequestRepository extends JpaRepository<Request, Long> {

    /**
     * Поиск всех запросов пользователя
     * @param requesterId
     * @return объект Request
     */
    List<Request> findAllByRequesterId(Long requesterId);

    /**
     * Подсчет количества подтвержденных заявок
     * @param eventId
     * @param status
     * @return Long
     */
    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    /**
     * Проверка на дубликаты
     * @param requesterId
     * @param eventId
     * @return Boolean
     */
    Boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    /**
     * Получение списка заявок для конкретного события
     * @param eventId
     * @return список объектов с типом Request
     */
    List<Request> findAllByEventId(Long eventId);

    /**
     * Получение списка заявок по списку их ID
     * @param ids
     * @return список объектов с типом Request
     */
    List<Request> findAllByIdIn(List<Long> ids);

    /**
     * Подсчет подтвержденных запросов по ID событий
     * @param eventIds
     * @return список объектов с типом ConfirmedRequestsView
     */
    @Query("SELECT new ru.practicum.request.dto.ConfirmedRequestsView (r.event.id, COUNT(r)) " +
            "FROM Request r " +
            "WHERE r.event.id IN :eventIds " +
            "AND r.status = 'CONFIRMED' " +
            "GROUP BY r.event.id")
    List<ConfirmedRequestsView> countConfirmedRequestsByEventIds(@Param("eventIds") Set<Long> eventIds);

    /**
     * Проверяем, что пользователь посетил событие (имел подтвержденную заявку)
     * @param requesterId
     * @param eventId
     * @param status
     * @return Boolean
     */
    Boolean existsByRequesterIdAndEventIdAndStatus(Long requesterId, Long eventId, RequestStatus status);
}
