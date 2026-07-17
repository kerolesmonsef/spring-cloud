package com.keroles.ddd.catalog.application;

import com.keroles.ddd.catalog.application.dto.CourseResponse;
import com.keroles.ddd.catalog.application.dto.TeacherResponse;
import com.keroles.ddd.catalog.domain.model.Course;
import com.keroles.ddd.catalog.domain.event.CoursePublishedEvent;
import com.keroles.ddd.catalog.domain.repository.CourseRepository;
import com.keroles.ddd.catalog.domain.model.Money;
import com.keroles.ddd.catalog.domain.model.Teacher;
import com.keroles.ddd.catalog.domain.repository.TeacherRepository;
import com.keroles.ddd.sharedkernel.DomainException;
import com.keroles.ddd.sharedkernel.PersonName;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogService {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TeacherResponse registerTeacher(String name) {
        Teacher teacher = Teacher.register(PersonName.of(name));
        return TeacherResponse.from(teacherRepository.save(teacher));
    }

    public CourseResponse authorCourse(String title, String description, BigDecimal price, Long teacherId) {
        teacherRepository.findById(teacherId)
                .orElseThrow(() -> new DomainException("Teacher " + teacherId + " not found"));
        Course course = Course.author(title, description, Money.of(price), teacherId);
        return CourseResponse.from(courseRepository.save(course));
    }

    public CourseResponse publishCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new DomainException("Course " + courseId + " not found"));
        course.publish();
        courseRepository.save(course);
        eventPublisher.publishEvent(CoursePublishedEvent.of(course.getId()));
        return CourseResponse.from(course);
    }

    @Transactional(readOnly = true)
    public CourseResponse findCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new DomainException("Course " + courseId + " not found"));
        return CourseResponse.from(course);
    }
}
