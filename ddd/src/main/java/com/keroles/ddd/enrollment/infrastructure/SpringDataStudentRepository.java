package com.keroles.ddd.enrollment.infrastructure;

import com.keroles.ddd.enrollment.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStudentRepository extends JpaRepository<Student, Long> {
}
