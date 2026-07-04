package com.keroles.ddd.enrollment.infrastructure;

import com.keroles.ddd.catalog.domain.Course;
import com.keroles.ddd.catalog.domain.CourseRepository;
import com.keroles.ddd.enrollment.domain.CourseCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatalogCourseAdapter implements CourseCatalog {

    private final CourseRepository courseRepository;

    @Override
    public boolean exists(Long courseId) {
        return courseRepository.findById(courseId).isPresent();
    }

    @Override
    public boolean isPublished(Long courseId) {
        return courseRepository.findById(courseId)
                .map(Course::isPublished)
                .orElse(false);
    }
}
