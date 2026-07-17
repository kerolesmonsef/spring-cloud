package com.keroles.ddd.catalog.infrastructure.persistence;

import com.keroles.ddd.catalog.domain.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCourseRepository extends JpaRepository<Course, Long> {
}
