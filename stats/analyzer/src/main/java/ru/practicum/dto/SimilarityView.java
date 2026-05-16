package ru.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimilarityView {

    private Double rating;
    private Double similarity;
}
