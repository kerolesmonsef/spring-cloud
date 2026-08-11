package com.keroles.wso2server.user.repository;

import com.keroles.wso2server.user.model.User;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUsername(String username) {
        return jdbcTemplate.query(
                        "SELECT id, username, pass FROM users WHERE username = ?",
                        (resultSet, rowNum) -> new User(
                                resultSet.getLong("id"),
                                resultSet.getString("username"),
                                resultSet.getString("pass")),
                        username)
                .stream()
                .findFirst();
    }
}
