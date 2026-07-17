package com.gqm2026.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthApplication {
    static { new java.io.File("data").mkdirs(); }
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
