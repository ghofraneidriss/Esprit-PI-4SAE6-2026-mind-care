package tn.esprit.forums_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.forums_service.dto.ForumStatsDto;
import tn.esprit.forums_service.service.ForumStatsService;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ForumStatsController {

    private final ForumStatsService forumStatsService;

    @GetMapping("/stats")
    public ForumStatsDto getStats() {
        return forumStatsService.buildStats();
    }
}
