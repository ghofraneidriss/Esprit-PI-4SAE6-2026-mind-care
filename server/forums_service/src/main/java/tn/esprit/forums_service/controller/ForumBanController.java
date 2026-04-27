package tn.esprit.forums_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.forums_service.dto.ForumBanStatusDto;
import tn.esprit.forums_service.service.ForumBanService;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ForumBanController {

    private final ForumBanService forumBanService;

    @GetMapping("/ban-status")
    public ForumBanStatusDto banStatus(@RequestParam Long userId) {
        return forumBanService.status(userId);
    }
}
