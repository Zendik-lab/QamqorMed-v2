package com.qamqormed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QamqorMedApplication {

    public static void main(String[] args) {
        SpringApplication.run(QamqorMedApplication.class, args);
    }
}
