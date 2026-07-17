package com.keroles.ddd.enrollment.infrastructure.persistence;

import com.keroles.ddd.enrollment.domain.model.Enrollment;
import com.keroles.ddd.enrollment.domain.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaEnrollmentRepositoryAdapter implements EnrollmentRepository {

    private final SpringDataEnrollmentRepository springDataRepository;

    @Override
    public Enrollment save(Enrollment enrollment) {
        return springDataRepository.save(enrollment);
    }

    @Override
    public Optional<Enrollment> findById(Long id) {
        return springDataRepository.findById(id);
    }

    @Override
    public boolean existsByStudentIdAndCourseId(Long studentId, Long courseId) {
        return springDataRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }
}
