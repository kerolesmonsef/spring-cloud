package com.keroles.ddd.enrollment.domain;

import java.util.Optional;

public interface EnrollmentRepository {

    Enrollment save(Enrollment enrollment);

    Optional<Enrollment> findById(Long id);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
