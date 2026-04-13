package tn.esprit.microservice.coursms.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.microservice.coursms.dto.CoursSummary;
import tn.esprit.microservice.coursms.entity.Cours;
import tn.esprit.microservice.coursms.service.CoursService;

@RestController
@RequestMapping("/cours")
public class CoursController {

    private final CoursService coursService;

    public CoursController(CoursService coursService) {
        this.coursService = coursService;
    }

    @GetMapping
    public List<Cours> getAllCours() {
        return coursService.getAllCours();
    }

    @GetMapping("/{id}")
    public Cours getCoursById(@PathVariable Long id) {
        return coursService.getCoursById(id);
    }

    @GetMapping("/{id}/summary")
    public CoursSummary getCoursSummary(@PathVariable Long id) {
        return coursService.getCoursSummary(id);
    }

    @PostMapping
    public ResponseEntity<Cours> createCours(@RequestBody Cours cours) {
        return ResponseEntity.ok(coursService.createCours(cours));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cours> updateCours(@PathVariable Long id, @RequestBody Cours cours) {
        return ResponseEntity.ok(coursService.updateCours(id, cours));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCours(@PathVariable Long id) {
        coursService.deleteCours(id);
        return ResponseEntity.noContent().build();
    }
}
