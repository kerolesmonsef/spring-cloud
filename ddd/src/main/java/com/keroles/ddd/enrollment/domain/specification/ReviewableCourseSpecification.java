package com.keroles.ddd.enrollment.domain.specification;

import com.keroles.ddd.enrollment.domain.repository.EnrollmentRepository;
import com.keroles.ddd.enrollment.domain.repository.ReviewRepository;

public class ReviewableCourseSpecification {

    private final EnrollmentRepository enrollmentRepository;
    private final ReviewRepository reviewRepository;

    public ReviewableCourseSpecification(EnrollmentRepository enrollmentRepository, ReviewRepository reviewRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.reviewRepository = reviewRepository;
    }

    public boolean isSatisfiedBy(Long studentId, Long courseId) {
        boolean enrolled = enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
        boolean alreadyReviewed = reviewRepository.existsByStudentIdAndCourseId(studentId, courseId);
        return enrolled && !alreadyReviewed;
    }
}
