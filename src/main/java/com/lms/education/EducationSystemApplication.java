package com.lms.education;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EducationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducationSystemApplication.class, args);
    }

}
