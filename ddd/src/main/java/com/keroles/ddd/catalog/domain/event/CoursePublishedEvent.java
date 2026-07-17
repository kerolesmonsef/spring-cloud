package com.keroles.ddd.catalog.domain.event;

import java.time.LocalDateTime;

public record CoursePublishedEvent(Long courseId, LocalDateTime occurredOn) {

    public static CoursePublishedEvent of(Long courseId) {
        return new CoursePublishedEvent(courseId, LocalDateTime.now());
    }
}
