package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Similarity;

import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {
    Optional<Similarity> findByEvent1AndEvent2(Long event1, Long event2);

    @Query("select s " +
            "from Similarity s " +
            "where s.event1 = :eventId " +
            "or s.event2 = :eventId")
    List<Similarity> findByEventId(@Param("eventId") Long eventId);

    @Query("select s " +
            "from Similarity s " +
            "where s.event1 in :eventIds " +
            "or s.event2 in :eventIds")
    List<Similarity> findByEventIds(@Param("eventIds") List<Long> eventIds);
}
