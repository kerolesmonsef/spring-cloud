package com.keroles.ddd.enrollment.application.dto;

import com.keroles.ddd.enrollment.domain.Review;

public record ReviewResponse(
        Long id,
        Long studentId,
        Long courseId,
        int rating,
        String comment) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getStudentId(),
                review.getCourseId(),
                review.getRating().getValue(),
                review.getComment());
    }
}
