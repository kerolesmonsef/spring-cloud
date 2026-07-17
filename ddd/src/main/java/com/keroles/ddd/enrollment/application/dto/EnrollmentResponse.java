package com.keroles.ddd.enrollment.application.dto;

import com.keroles.ddd.enrollment.domain.model.Enrollment;

public record EnrollmentResponse(
        Long id,
        Long studentId,
        Long courseId,
        String status,
        int progressPercent) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudentId(),
                enrollment.getCourseId(),
                enrollment.getStatus().name(),
                enrollment.getProgress().getPercent());
    }
}
