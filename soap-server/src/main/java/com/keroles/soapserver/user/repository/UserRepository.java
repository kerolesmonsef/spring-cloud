package com.keroles.soapserver.user.repository;

import com.keroles.soapserver.user.model.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
