package tn.esprit.forums_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.forums_service.entity.Category;
import tn.esprit.forums_service.repository.CategoryRepository;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        log.info("[DataInitializer] Checking if categories need to be initialized...");

        long count = categoryRepository.count();
        if (count == 0) {
            log.info("[DataInitializer] No categories found. Initializing default categories...");

            List<Category> defaultCategories = Arrays.asList(
                Category.builder()
                    .name("Early Signs & Symptoms")
                    .description("Discuss early warning signs, memory changes, and when to seek medical advice. Share experiences and learn about initial symptoms.")
                    .build(),
                Category.builder()
                    .name("Caregiver Support")
                    .description("A safe space for caregivers to share challenges, coping strategies, and find emotional support from those who understand.")
                    .build(),
                Category.builder()
                    .name("Treatment & Research")
                    .description("Latest research findings, treatment options, clinical trials, and medical breakthroughs in Alzheimer's care.")
                    .build(),
                Category.builder()
                    .name("Daily Living Tips")
                    .description("Practical advice for managing daily activities, home safety, nutrition, and maintaining quality of life.")
                    .build(),
                Category.builder()
                    .name("Legal & Financial")
                    .description("Guidance on legal planning, financial management, insurance, power of attorney, and navigating healthcare systems.")
                    .build(),
                Category.builder()
                    .name("Memory Cafe")
                    .description("Social space for casual conversation, shared interests, success stories, and community connection.")
                    .build()
            );

            categoryRepository.saveAll(defaultCategories);
            log.info("[DataInitializer] Successfully created {} default categories", defaultCategories.size());
        } else {
            log.info("[DataInitializer] Categories already exist ({} found). Skipping initialization.", count);
        }
    }
}
