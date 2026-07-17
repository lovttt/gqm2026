package com.gqm2026.school;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SchoolApplication {
    static { new java.io.File("data").mkdirs(); }
    public static void main(String[] args) {
        SpringApplication.run(SchoolApplication.class, args);
    }
}
