package com.keroles.ddd.catalog.infrastructure;

import com.keroles.ddd.catalog.domain.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTeacherRepository extends JpaRepository<Teacher, Long> {
}
