package com.example.volrabbit_be;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling // 마감기한 지난 투두들 자동 fail 처리
@SpringBootApplication
public class VolrabbitBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolrabbitBeApplication.class, args);
    }
}