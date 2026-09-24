package com.example.logistics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class LogisticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsServiceApplication.class, args);
    }
}
