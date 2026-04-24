package tn.esprit.medical_report_service.Clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.medical_report_service.DTOs.UserDTO;

@FeignClient(name = "users-service", url = "${users.service.url}")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}

