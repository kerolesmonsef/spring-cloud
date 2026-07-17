package com.keroles.ddd.catalog.domain.repository;

import com.keroles.ddd.catalog.domain.model.Teacher;

import java.util.Optional;

public interface TeacherRepository {

    Teacher save(Teacher teacher);

    Optional<Teacher> findById(Long id);
}
