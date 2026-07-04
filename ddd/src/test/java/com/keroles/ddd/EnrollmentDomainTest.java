package com.keroles.ddd;

import com.keroles.ddd.enrollment.domain.CourseCatalog;
import com.keroles.ddd.enrollment.domain.Enrollment;
import com.keroles.ddd.enrollment.domain.EnrollmentPolicy;
import com.keroles.ddd.enrollment.domain.EnrollmentRepository;
import com.keroles.ddd.enrollment.domain.Progress;
import com.keroles.ddd.enrollment.domain.Rating;
import com.keroles.ddd.sharedkernel.DomainException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnrollmentDomainTest {

    @Test
    void ratingRejectsOutOfRange() {
        assertThrows(DomainException.class, () -> Rating.of(0));
        assertThrows(DomainException.class, () -> Rating.of(6));
        assertEquals(5, Rating.of(5).getValue());
    }

    @Test
    void progressCannotMoveBackwards() {
        Progress p = Progress.of(40);
        assertThrows(DomainException.class, () -> p.advanceTo(30));
        assertEquals(80, p.advanceTo(80).getPercent());
    }

    @Test
    void cannotEnrollInUnpublishedCourse() {
        EnrollmentPolicy policy = new EnrollmentPolicy(catalog(true, false), repo(false));
        assertThrows(DomainException.class, () -> policy.enroll(1L, 1L));
    }

    @Test
    void cannotEnrollTwice() {
        EnrollmentPolicy policy = new EnrollmentPolicy(catalog(true, true), repo(true));
        assertThrows(DomainException.class, () -> policy.enroll(1L, 1L));
    }

    @Test
    void enrollsWhenPublishedAndNotYetEnrolled() {
        EnrollmentPolicy policy = new EnrollmentPolicy(catalog(true, true), repo(false));
        Enrollment enrollment = policy.enroll(7L, 9L);
        assertEquals(7L, enrollment.getStudentId());
        assertEquals(9L, enrollment.getCourseId());
        assertEquals(0, enrollment.getProgress().getPercent());
    }

    private CourseCatalog catalog(boolean exists, boolean published) {
        return new CourseCatalog() {
            public boolean exists(Long courseId) {
                return exists;
            }

            public boolean isPublished(Long courseId) {
                return published;
            }
        };
    }

    private EnrollmentRepository repo(boolean alreadyEnrolled) {
        return new EnrollmentRepository() {
            public Enrollment save(Enrollment enrollment) {
                return enrollment;
            }

            public Optional<Enrollment> findById(Long id) {
                return Optional.empty();
            }

            public boolean existsByStudentIdAndCourseId(Long studentId, Long courseId) {
                return alreadyEnrolled;
            }

            public List<Enrollment> findByStudentId(Long studentId) {
                return List.of();
            }
        };
    }
}
