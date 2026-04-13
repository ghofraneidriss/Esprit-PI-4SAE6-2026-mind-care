package tn.esprit.microservice.inscriptionms.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.microservice.inscriptionms.dto.CoursSummary;

@FeignClient(name = "cours-ms")
public interface CoursClient {

    @GetMapping("/cours/{id}/summary")
    CoursSummary getCoursSummary(@PathVariable("id") Long id);
}
