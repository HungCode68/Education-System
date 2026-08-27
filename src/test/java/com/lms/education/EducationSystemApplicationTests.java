package com.lms.education;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EducationSystemApplicationTests {

    @Test
    void contextLoads() {
        System.out.println("BCRYPT HASH FOR 123456: " + new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));
    }
}
