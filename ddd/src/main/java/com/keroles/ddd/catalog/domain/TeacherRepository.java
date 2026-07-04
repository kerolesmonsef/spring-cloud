package com.keroles.ddd.catalog.domain;

import java.util.Optional;

public interface TeacherRepository {

    Teacher save(Teacher teacher);

    Optional<Teacher> findById(Long id);
}
