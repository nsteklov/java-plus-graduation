package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.SimilarityView;
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


    @Query ("select new ru.practicum.dto.SimilarityView (i.rating, s.similarity) " +
            "from Interaction i " +
            "join Similarity s " +
            "on i.eventId = s.event1 " +
            "where s.event2 = :eventId " +
            "and i.userId = :userId " +
            "union all " +
            "select new ru.practicum.dto.SimilarityView (i.rating, s.similarity) " +
            "from Interaction i " +
            "join Similarity s " +
            "on i.eventId = s.event2 " +
            "where s.event1 = :eventId " +
            "and i.userId = :userId " +
            "order by s.similarity desc")
    Page<SimilarityView> findSimilar(Long eventId, Long userId, Pageable pageable);
}
