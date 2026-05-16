package ru.practicum.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "similarities", schema = "public")
@Data
public class Similarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long event1;

    @Column(nullable = false)
    private Long event2;

    @Column
    private Double similarity;

    @Column
    private Instant ts;
}
