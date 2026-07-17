package com.keroles.ddd.enrollment.domain.repository;

import com.keroles.ddd.enrollment.domain.model.Student;

import java.util.Optional;

public interface StudentRepository {

    Student save(Student student);

    Optional<Student> findById(Long id);
}
