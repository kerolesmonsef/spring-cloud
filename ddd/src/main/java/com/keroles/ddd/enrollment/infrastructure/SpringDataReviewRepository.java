package com.keroles.ddd.enrollment.infrastructure;

import com.keroles.ddd.enrollment.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Review> findByCourseId(Long courseId);
}
