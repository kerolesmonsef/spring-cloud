package com.keroles.ddd.enrollment.domain.repository;

import com.keroles.ddd.enrollment.domain.model.Enrollment;

import java.util.Optional;

public interface EnrollmentRepository {

    Enrollment save(Enrollment enrollment);

    Optional<Enrollment> findById(Long id);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
