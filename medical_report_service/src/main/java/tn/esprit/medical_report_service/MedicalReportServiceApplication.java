package tn.esprit.medical_report_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MedicalReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalReportServiceApplication.class, args);
    }

}
