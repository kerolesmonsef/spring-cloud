package com.keroles.ddd.enrollment.infrastructure.persistence;

import com.keroles.ddd.enrollment.domain.model.Review;
import com.keroles.ddd.enrollment.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaReviewRepositoryAdapter implements ReviewRepository {

    private final SpringDataReviewRepository springDataRepository;

    @Override
    public Review save(Review review) {
        return springDataRepository.save(review);
    }

    @Override
    public Optional<Review> findById(Long id) {
        return springDataRepository.findById(id);
    }

    @Override
    public boolean existsByStudentIdAndCourseId(Long studentId, Long courseId) {
        return springDataRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    @Override
    public List<Review> findByCourseId(Long courseId) {
        return springDataRepository.findByCourseId(courseId);
    }
}
