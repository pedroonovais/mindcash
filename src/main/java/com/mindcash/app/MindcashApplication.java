package com.mindcash.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MindcashApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindcashApplication.class, args);
    }
}
