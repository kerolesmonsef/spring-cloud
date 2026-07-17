package com.keroles.ddd.enrollment.infrastructure.persistence;

import com.keroles.ddd.enrollment.domain.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataEnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
