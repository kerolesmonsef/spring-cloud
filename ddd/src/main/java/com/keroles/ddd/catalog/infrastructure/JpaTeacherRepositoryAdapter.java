package com.keroles.ddd.catalog.infrastructure;

import com.keroles.ddd.catalog.domain.Teacher;
import com.keroles.ddd.catalog.domain.TeacherRepository;
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
