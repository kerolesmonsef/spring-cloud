package com.keroles.configclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Component
public class DatabaseConfigPrinter implements CommandLineRunner {

    @Value("${database.username}")
    private String databaseUser;

    @Value("${database.password}")
    private String databasePassword;


    @Override
    public void run(String... args) {
        System.out.println("Database User: " + databaseUser);
        System.out.println("Database Password: " + databasePassword);
    }
}