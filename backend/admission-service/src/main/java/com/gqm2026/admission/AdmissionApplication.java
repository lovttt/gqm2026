package com.gqm2026.admission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AdmissionApplication {
    static { new java.io.File("data").mkdirs(); }
    public static void main(String[] args) {
        SpringApplication.run(AdmissionApplication.class, args);
    }
}
