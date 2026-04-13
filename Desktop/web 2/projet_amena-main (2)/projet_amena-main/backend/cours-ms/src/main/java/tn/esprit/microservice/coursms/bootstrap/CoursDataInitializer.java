package tn.esprit.microservice.coursms.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tn.esprit.microservice.coursms.entity.Cours;
import tn.esprit.microservice.coursms.entity.CourseStatus;
import tn.esprit.microservice.coursms.entity.Level;
import tn.esprit.microservice.coursms.repository.CoursRepository;

@Configuration
public class CoursDataInitializer {

    @Bean
    CommandLineRunner loadCours(CoursRepository coursRepository) {
        return args -> {
            if (coursRepository.count() > 0) {
                return;
            }

            Cours javaCourse = new Cours();
            javaCourse.setTitle("Java for Beginners");
            javaCourse.setDescription("Introduction to Java and OOP concepts");
            javaCourse.setCategory("Programming");
            javaCourse.setLanguage("English");
            javaCourse.setLevel(Level.BEGINNER);
            javaCourse.setPrice("49");
            javaCourse.setDuration("12h");
            javaCourse.setInstructor("Amena Trainer");
            javaCourse.setStatus(CourseStatus.PUBLISHED);

            Cours springCourse = new Cours();
            springCourse.setTitle("Spring Boot Microservices");
            springCourse.setDescription("Build distributed systems with Spring Cloud");
            springCourse.setCategory("Backend");
            springCourse.setLanguage("French");
            springCourse.setLevel(Level.ADVANCED);
            springCourse.setPrice("89");
            springCourse.setDuration("18h");
            springCourse.setInstructor("Esprit Team");
            springCourse.setStatus(CourseStatus.PUBLISHED);

            coursRepository.save(javaCourse);
            coursRepository.save(springCourse);
        };
    }
}
