package com.keroles.ddd.enrollment.infrastructure.persistence;

import com.keroles.ddd.enrollment.domain.model.Student;
import com.keroles.ddd.enrollment.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaStudentRepositoryAdapter implements StudentRepository {

    private final SpringDataStudentRepository springDataRepository;

    @Override
    public Student save(Student student) {
        return springDataRepository.save(student);
    }

    @Override
    public Optional<Student> findById(Long id) {
        return springDataRepository.findById(id);
    }
}
