package com.keroles.ddd.enrollment.infrastructure.acl;

import com.keroles.ddd.catalog.domain.model.Course;
import com.keroles.ddd.catalog.domain.repository.CourseRepository;
import com.keroles.ddd.enrollment.domain.repository.CourseCatalog;
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
