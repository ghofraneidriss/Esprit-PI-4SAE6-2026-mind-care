package tn.esprit.forums_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tn.esprit.forums_service.entity.Category;
import tn.esprit.forums_service.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed MindCare forum categories (Alzheimer’s disease & dementia support).
 * Icons: Remix Icon CSS classes for the Angular front.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    private record CatDef(String name, String description, String icon, String color) {}

    /** Six MindCare forum themes (public browse is limited to these). */
    private static final List<CatDef> FIXED = List.of(
            new CatDef(
                    "Early Signs & Symptoms",
                    "Alzheimer’s disease and dementia: early warning signs, memory changes, and when to seek medical evaluation.",
                    "ri-heart-pulse-line",
                    "#0891b2"
            ),
            new CatDef(
                    "Caregiver Support",
                    "Support for family and professional caregivers: daily challenges, coping, respite, and emotional well-being in Alzheimer’s care.",
                    "ri-hand-heart-line",
                    "#f43f5e"
            ),
            new CatDef(
                    "Treatment & Research",
                    "Approved treatments, clinical trials, biomarkers, and research news in Alzheimer’s and related dementias.",
                    "ri-microscope-line",
                    "#6366f1"
            ),
            new CatDef(
                    "Daily Living Tips",
                    "Practical routines, home safety, nutrition, sleep, and quality of life while living with Alzheimer’s disease.",
                    "ri-home-heart-line",
                    "#059669"
            ),
            new CatDef(
                    "Legal & Financial",
                    "Legal capacity, advance directives, insurance, care costs, and navigating health and social systems.",
                    "ri-scales-3-line",
                    "#d97706"
            ),
            new CatDef(
                    "Memory Cafe",
                    "Community activities, memory cafés, hobbies, and staying socially connected with dementia.",
                    "ri-cup-line",
                    "#db2777"
            )
    );

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            List<Category> batch = new ArrayList<>(FIXED.size());
            for (CatDef def : FIXED) {
                batch.add(Category.builder()
                        .name(def.name())
                        .description(def.description())
                        .icon(def.icon())
                        .color(def.color())
                        .build());
            }
            categoryRepository.saveAll(batch);
            log.info("[DataInitializer] Seeded {} forum categories (empty DB, bulk insert).", batch.size());
            return;
        }

        log.info("[DataInitializer] Syncing {} fixed forum categories…", FIXED.size());
        for (CatDef def : FIXED) {
            categoryRepository.findByName(def.name()).ifPresentOrElse(
                    existing -> {
                        boolean changed = false;
                        if (existing.getIcon() == null || !def.icon().equals(existing.getIcon())) {
                            existing.setIcon(def.icon());
                            changed = true;
                        }
                        if (existing.getColor() == null || !def.color().equals(existing.getColor())) {
                            existing.setColor(def.color());
                            changed = true;
                        }
                        if (existing.getDescription() == null || !def.description().equals(existing.getDescription())) {
                            existing.setDescription(def.description());
                            changed = true;
                        }
                        if (changed) {
                            categoryRepository.save(existing);
                            log.info("[DataInitializer] Updated category: {}", def.name());
                        }
                    },
                    () -> {
                        categoryRepository.save(Category.builder()
                                .name(def.name())
                                .description(def.description())
                                .icon(def.icon())
                                .color(def.color())
                                .build());
                        log.info("[DataInitializer] Created category: {}", def.name());
                    }
            );
        }
    }
}
