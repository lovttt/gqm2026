package com.gqm2026.student.dto;

import com.gqm2026.student.entity.Application;
import com.gqm2026.student.entity.Student;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDataset {
    private List<Student> students;
    private List<Application> applications;
}
