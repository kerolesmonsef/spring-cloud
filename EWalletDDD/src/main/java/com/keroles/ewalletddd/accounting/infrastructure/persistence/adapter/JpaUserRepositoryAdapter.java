package com.keroles.ewalletddd.accounting.infrastructure.persistence.adapter;
  import com.keroles.ewalletddd.accounting.infrastructure.persistence.entity.UserJpaEntity; import com.keroles.ewalletddd.accounting.infrastructure.persistence.repository.SpringDataUserJpa;

import com.keroles.ewalletddd.accounting.domain.model.User;
import com.keroles.ewalletddd.accounting.domain.repository.UserRepository;
import com.keroles.ewalletddd.shared.domain.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaUserRepositoryAdapter implements UserRepository {

    private final SpringDataUserJpa jpa;

    public JpaUserRepositoryAdapter(SpringDataUserJpa jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value())
                .map(row -> User.restore(new UserId(row.getId()), row.getCreatedAt()));
    }

    @Override
    public void save(User user) {
        UserJpaEntity row = jpa.findById(user.id().value()).orElseGet(UserJpaEntity::new);
        row.setId(user.id().value());
        row.setCreatedAt(user.createdAt());
        jpa.save(row);
    }
}
