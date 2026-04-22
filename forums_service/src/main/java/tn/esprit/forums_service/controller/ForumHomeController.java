package tn.esprit.forums_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.forums_service.dto.ForumHomeDTO;
import tn.esprit.forums_service.service.ForumHomeService;

/**
 * Single payload for the public forum home (categories, KPIs, sorted threads, top widgets) — one HTTP round-trip.
 */
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ForumHomeController {

    private final ForumHomeService forumHomeService;

    /**
     * @param postSort {@code recent} | {@code hot} (by comments) | {@code views}
     * @param postPage 0-based page for the main thread list
     * @param postSize page size (max 48)
     */
    @GetMapping("/home")
    public ForumHomeDTO home(
            @RequestParam(defaultValue = "recent") String postSort,
            @RequestParam(defaultValue = "0") int postPage,
            @RequestParam(defaultValue = "12") int postSize,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId) {
        return forumHomeService.buildHome(postSort, postPage, postSize, q, categoryId);
    }
}
