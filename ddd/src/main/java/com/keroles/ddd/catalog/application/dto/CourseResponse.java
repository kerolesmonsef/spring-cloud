package com.keroles.ddd.catalog.application.dto;

import com.keroles.ddd.catalog.domain.model.Course;

import java.math.BigDecimal;

public record CourseResponse(
        Long id,
        String title,
        String description,
        BigDecimal price,
        Long teacherId,
        String status) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getPrice().getAmount(),
                course.getTeacherId(),
                course.getStatus().name());
    }
}
