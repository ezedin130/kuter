package com.kuter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entrypoint for Kuter Building Materials Inventory Management SaaS.
 */
@SpringBootApplication
@EnableScheduling
public class KuterApplication {

    public static void main(String[] args) {
        SpringApplication.run(KuterApplication.class, args);
    }
}
