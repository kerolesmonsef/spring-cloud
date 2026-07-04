package com.keroles.ddd.enrollment.application;

import com.keroles.ddd.enrollment.application.dto.EnrollmentResponse;
import com.keroles.ddd.enrollment.application.dto.ReviewResponse;
import com.keroles.ddd.enrollment.application.dto.StudentResponse;
import com.keroles.ddd.enrollment.domain.CourseCatalog;
import com.keroles.ddd.enrollment.domain.Enrollment;
import com.keroles.ddd.enrollment.domain.EnrollmentPolicy;
import com.keroles.ddd.enrollment.domain.EnrollmentRepository;
import com.keroles.ddd.enrollment.domain.Rating;
import com.keroles.ddd.enrollment.domain.Review;
import com.keroles.ddd.enrollment.domain.ReviewRepository;
import com.keroles.ddd.enrollment.domain.ReviewableCourseSpecification;
import com.keroles.ddd.enrollment.domain.Student;
import com.keroles.ddd.enrollment.domain.StudentEnrolledEvent;
import com.keroles.ddd.enrollment.domain.StudentRepository;
import com.keroles.ddd.sharedkernel.DomainException;
import com.keroles.ddd.sharedkernel.PersonName;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EnrollmentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ReviewRepository reviewRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EnrollmentPolicy enrollmentPolicy;
    private final ReviewableCourseSpecification reviewableCourse;

    public EnrollmentService(
            StudentRepository studentRepository,
            EnrollmentRepository enrollmentRepository,
            ReviewRepository reviewRepository,
            CourseCatalog courseCatalog,
            ApplicationEventPublisher eventPublisher) {
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.reviewRepository = reviewRepository;
        this.eventPublisher = eventPublisher;
        this.enrollmentPolicy = new EnrollmentPolicy(courseCatalog, enrollmentRepository);
        this.reviewableCourse = new ReviewableCourseSpecification(enrollmentRepository, reviewRepository);
    }

    public StudentResponse registerStudent(String name) {
        Student student = Student.register(PersonName.of(name));
        return StudentResponse.from(studentRepository.save(student));
    }

    public EnrollmentResponse enroll(Long studentId, Long courseId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new DomainException("Student " + studentId + " not found"));
        Enrollment enrollment = enrollmentPolicy.enroll(studentId, courseId);
        enrollment = enrollmentRepository.save(enrollment);
        eventPublisher.publishEvent(StudentEnrolledEvent.of(studentId, courseId));
        return EnrollmentResponse.from(enrollment);
    }

    public EnrollmentResponse advanceProgress(Long enrollmentId, int percent) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new DomainException("Enrollment " + enrollmentId + " not found"));
        enrollment.advanceProgress(percent);
        return EnrollmentResponse.from(enrollmentRepository.save(enrollment));
    }

    public ReviewResponse writeReview(Long studentId, Long courseId, int rating, String comment) {
        if (!reviewableCourse.isSatisfiedBy(studentId, courseId)) {
            throw new DomainException("Only an enrolled student who has not reviewed yet can review this course");
        }
        Review review = Review.write(studentId, courseId, Rating.of(rating), comment);
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> reviewsOf(Long courseId) {
        return reviewRepository.findByCourseId(courseId).stream()
                .map(ReviewResponse::from)
                .toList();
    }
}
