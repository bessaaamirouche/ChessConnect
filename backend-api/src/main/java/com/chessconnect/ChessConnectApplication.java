package com.chessconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChessConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChessConnectApplication.class, args);
    }
}
