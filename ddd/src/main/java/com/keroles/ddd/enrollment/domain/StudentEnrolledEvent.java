package com.keroles.ddd.enrollment.domain;

import java.time.LocalDateTime;

public record StudentEnrolledEvent(Long studentId, Long courseId, LocalDateTime occurredOn) {

    public static StudentEnrolledEvent of(Long studentId, Long courseId) {
        return new StudentEnrolledEvent(studentId, courseId, LocalDateTime.now());
    }
}
