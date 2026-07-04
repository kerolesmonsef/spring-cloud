package com.keroles.ddd.enrollment.application.dto;

import com.keroles.ddd.enrollment.domain.Student;

public record StudentResponse(Long id, String name) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getName().getValue());
    }
}
