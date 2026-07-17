package com.keroles.ddd.catalog.infrastructure.persistence;

import com.keroles.ddd.catalog.domain.model.Teacher;
import com.keroles.ddd.catalog.domain.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaTeacherRepositoryAdapter implements TeacherRepository {

    private final SpringDataTeacherRepository springDataRepository;

    @Override
    public Teacher save(Teacher teacher) {
        return springDataRepository.save(teacher);
    }

    @Override
    public Optional<Teacher> findById(Long id) {
        return springDataRepository.findById(id);
    }
}
