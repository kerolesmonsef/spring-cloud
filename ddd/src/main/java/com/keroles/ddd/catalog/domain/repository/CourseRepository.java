package com.keroles.ddd.catalog.domain.repository;

import com.keroles.ddd.catalog.domain.model.Course;

import java.util.Optional;

public interface CourseRepository {

    Course save(Course course);

    Optional<Course> findById(Long id);
}
