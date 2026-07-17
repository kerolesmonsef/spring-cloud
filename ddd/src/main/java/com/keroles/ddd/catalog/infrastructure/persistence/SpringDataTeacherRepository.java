package com.keroles.ddd.catalog.infrastructure.persistence;

import com.keroles.ddd.catalog.domain.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTeacherRepository extends JpaRepository<Teacher, Long> {
}
