package com.keroles.soapserver.user.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @jakarta.persistence.Column(unique = true, nullable = false)
    private String email;

    protected User() {
    }

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

}
