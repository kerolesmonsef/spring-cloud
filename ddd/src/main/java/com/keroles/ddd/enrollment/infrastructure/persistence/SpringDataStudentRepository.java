package com.keroles.ddd.enrollment.infrastructure.persistence;

import com.keroles.ddd.enrollment.domain.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStudentRepository extends JpaRepository<Student, Long> {
}
