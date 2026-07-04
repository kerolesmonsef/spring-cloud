package com.keroles.ddd.catalog.application.dto;

import com.keroles.ddd.catalog.domain.Teacher;

public record TeacherResponse(Long id, String name) {

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(teacher.getId(), teacher.getName().getValue());
    }
}
