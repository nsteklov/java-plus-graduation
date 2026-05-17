package ru.practicum.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.category.Category;

import java.time.LocalDateTime;

@Entity
@Table(name = "events", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String annotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(nullable = false, length = 7000)
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "initiator_id")
    private Long initiatorId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "lat")),
            @AttributeOverride(name = "lon", column = @Column(name = "lon"))
    })
    private Location location;

    @Column(name = "is_paid")
    private Boolean paid;

    @Column(name = "participant_limit")
    private Integer participantLimit;

    @Column(name = "published_on")
    private LocalDateTime publishedOn;

    @Column(name = "request_moderation")
    private Boolean requestModeration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "rating")
    private Double rating = 0.0;

    @Column(name = "likes")
    private Long likes = 0L;
}
