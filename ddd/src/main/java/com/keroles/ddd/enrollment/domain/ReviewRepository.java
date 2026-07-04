package com.keroles.ddd.enrollment.domain;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

    Review save(Review review);

    Optional<Review> findById(Long id);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Review> findByCourseId(Long courseId);
}
