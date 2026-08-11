package com.keroles.wso2server.user.repository;

import com.keroles.wso2server.user.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void findsSeedUserFromSqlite() {
        User user = userRepository.findByUsername("mbank").orElseThrow();

        assertThat(user.username()).isEqualTo("mbank");
        assertThat(user.password()).isEqualTo("123456");
    }
}
