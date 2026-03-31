package com.rapport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RapportApplication {
    public static void main(String[] args) {
        SpringApplication.run(RapportApplication.class, args);
    }
}
