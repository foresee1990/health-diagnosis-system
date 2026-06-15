package com.health.healthdiagnosis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@MapperScan("com.health.healthdiagnosis.mapper")
@SpringBootApplication
public class HealthDiagnosisSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthDiagnosisSystemApplication.class, args);
    }

}
