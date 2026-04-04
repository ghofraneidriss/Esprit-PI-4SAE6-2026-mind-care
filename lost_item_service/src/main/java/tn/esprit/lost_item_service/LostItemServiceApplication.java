package tn.esprit.lost_item_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LostItemServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LostItemServiceApplication.class, args);
    }
}
