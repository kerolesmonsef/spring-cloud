package com.keroles.ddd.catalog.api;

import com.keroles.ddd.catalog.application.CatalogService;
import com.keroles.ddd.catalog.application.dto.CourseResponse;
import com.keroles.ddd.catalog.application.dto.TeacherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @PostMapping("/teachers")
    public TeacherResponse registerTeacher(@RequestBody RegisterTeacherRequest request) {
        return catalogService.registerTeacher(request.name());
    }

    @PostMapping("/courses")
    public CourseResponse authorCourse(@RequestBody AuthorCourseRequest request) {
        return catalogService.authorCourse(
                request.title(), request.description(), request.price(), request.teacherId());
    }

    @PostMapping("/courses/{courseId}/publish")
    public CourseResponse publishCourse(@PathVariable Long courseId) {
        return catalogService.publishCourse(courseId);
    }

    @GetMapping("/courses/{courseId}")
    public CourseResponse findCourse(@PathVariable Long courseId) {
        return catalogService.findCourse(courseId);
    }

    public record RegisterTeacherRequest(String name) {
    }

    public record AuthorCourseRequest(String title, String description, BigDecimal price, Long teacherId) {
    }
}
