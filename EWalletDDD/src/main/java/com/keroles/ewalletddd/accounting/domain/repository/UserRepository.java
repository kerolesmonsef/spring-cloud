package com.keroles.ewalletddd.accounting.domain.repository;

import com.keroles.ewalletddd.accounting.domain.model.User;
import com.keroles.ewalletddd.shared.domain.UserId;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(UserId id);
    void save(User user);
}
