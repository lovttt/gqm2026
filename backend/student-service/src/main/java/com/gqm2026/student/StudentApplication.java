package com.gqm2026.student;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StudentApplication {
    static { new java.io.File("data").mkdirs(); }
    public static void main(String[] args) {
        SpringApplication.run(StudentApplication.class, args);
    }
}
