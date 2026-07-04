package com.keroles.ddd.enrollment.interfaces;

import com.keroles.ddd.enrollment.application.EnrollmentService;
import com.keroles.ddd.enrollment.application.dto.EnrollmentResponse;
import com.keroles.ddd.enrollment.application.dto.ReviewResponse;
import com.keroles.ddd.enrollment.application.dto.StudentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/enrollment")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/students")
    public StudentResponse registerStudent(@RequestBody RegisterStudentRequest request) {
        return enrollmentService.registerStudent(request.name());
    }

    @PostMapping("/enrollments")
    public EnrollmentResponse enroll(@RequestBody EnrollRequest request) {
        return enrollmentService.enroll(request.studentId(), request.courseId());
    }

    @PostMapping("/enrollments/{enrollmentId}/progress")
    public EnrollmentResponse advanceProgress(
            @PathVariable Long enrollmentId, @RequestBody ProgressRequest request) {
        return enrollmentService.advanceProgress(enrollmentId, request.percent());
    }

    @PostMapping("/reviews")
    public ReviewResponse writeReview(@RequestBody WriteReviewRequest request) {
        return enrollmentService.writeReview(
                request.studentId(), request.courseId(), request.rating(), request.comment());
    }

    @GetMapping("/courses/{courseId}/reviews")
    public List<ReviewResponse> reviews(@PathVariable Long courseId) {
        return enrollmentService.reviewsOf(courseId);
    }

    public record RegisterStudentRequest(String name) {
    }

    public record EnrollRequest(Long studentId, Long courseId) {
    }

    public record ProgressRequest(int percent) {
    }

    public record WriteReviewRequest(Long studentId, Long courseId, int rating, String comment) {
    }
}
