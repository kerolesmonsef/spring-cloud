package com.keroles.ddd.catalog.infrastructure;

import com.keroles.ddd.catalog.domain.Course;
import com.keroles.ddd.catalog.domain.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaCourseRepositoryAdapter implements CourseRepository {

    private final SpringDataCourseRepository springDataRepository;

    @Override
    public Course save(Course course) {
        return springDataRepository.save(course);
    }

    @Override
    public Optional<Course> findById(Long id) {
        return springDataRepository.findById(id);
    }
}
