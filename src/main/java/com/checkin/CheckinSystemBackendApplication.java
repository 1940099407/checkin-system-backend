package com.checkin;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication
@MapperScan("com.checkin.mapper")
public class CheckinSystemBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(CheckinSystemBackendApplication.class, args);
    }
}