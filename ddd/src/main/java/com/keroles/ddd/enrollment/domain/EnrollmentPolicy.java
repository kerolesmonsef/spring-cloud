package com.keroles.ddd.enrollment.domain;

import com.keroles.ddd.sharedkernel.DomainException;

public class EnrollmentPolicy {

    private final CourseCatalog courseCatalog;
    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentPolicy(CourseCatalog courseCatalog, EnrollmentRepository enrollmentRepository) {
        this.courseCatalog = courseCatalog;
        this.enrollmentRepository = enrollmentRepository;
    }

    public Enrollment enroll(Long studentId, Long courseId) {
        if (!courseCatalog.exists(courseId)) {
            throw new DomainException("Course " + courseId + " does not exist");
        }
        if (!courseCatalog.isPublished(courseId)) {
            throw new DomainException("Cannot enroll in a course that is not published");
        }
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new DomainException("Student is already enrolled in this course");
        }
        return Enrollment.open(studentId, courseId);
    }
}
