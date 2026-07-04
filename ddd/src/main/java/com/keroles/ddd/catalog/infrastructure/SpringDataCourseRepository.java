package com.keroles.ddd.catalog.infrastructure;

import com.keroles.ddd.catalog.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataCourseRepository extends JpaRepository<Course, Long> {
}
