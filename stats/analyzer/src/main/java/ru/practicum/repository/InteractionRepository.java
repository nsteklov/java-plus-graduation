package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Interaction;

import java.util.List;
import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    Optional<Interaction> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("select i.eventId " +
            "from Interaction i " +
            "where i.userId = :userId " +
            "and i.eventId in :eventIds ")
    List<Long> findByUserIdAndEventIds(@Param("userId") Long userId, @Param("eventIds") List<Long> eventIds);

    Page<Interaction> getByUserId(Long userId, Pageable pageable);
}
