package com.keroles.ddd.enrollment.domain;

import com.keroles.ddd.sharedkernel.DomainException;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "enrollment_enrollments")
@Getter
@NoArgsConstructor(access = PROTECTED)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;

    private Long courseId;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status;

    @Embedded
    private Progress progress;

    private Enrollment(Long studentId, Long courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.status = EnrollmentStatus.ACTIVE;
        this.progress = Progress.zero();
    }

    public static Enrollment open(Long studentId, Long courseId) {
        if (studentId == null) {
            throw new DomainException("Enrollment requires a student");
        }
        if (courseId == null) {
            throw new DomainException("Enrollment requires a course");
        }
        return new Enrollment(studentId, courseId);
    }

    public void advanceProgress(int percent) {
        if (status == EnrollmentStatus.COMPLETED) {
            throw new DomainException("A completed enrollment cannot change progress");
        }
        this.progress = this.progress.advanceTo(percent);
        if (this.progress.isComplete()) {
            this.status = EnrollmentStatus.COMPLETED;
        }
    }
}
