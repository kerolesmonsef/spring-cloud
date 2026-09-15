package com.keroles.soapserver.user.service;

import com.keroles.soapserver.user.model.User;
import com.keroles.soapserver.user.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(long id) {
        return userRepository.findById(id);
    }

    public User create(String name, String email) {
        return userRepository.save(new User(name, email));
    }

    public boolean update(long id, String name, String email) {
        return userRepository.findById(id).map(user -> {
            user.setName(name);
            user.setEmail(email);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public boolean delete(long id) {
        if (!userRepository.existsById(id)) {
            return false;
        }
        userRepository.deleteById(id);
        return true;
    }
}
