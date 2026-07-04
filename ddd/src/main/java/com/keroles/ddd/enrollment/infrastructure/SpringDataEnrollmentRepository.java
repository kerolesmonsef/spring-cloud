package com.keroles.ddd.enrollment.infrastructure;

import com.keroles.ddd.enrollment.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataEnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
