package tn.esprit.microservice.cours.Entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
public class Cours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String cover;
    private String video;
    private String category;
    private String language;
    @Enumerated(EnumType.STRING)
    private LEVEL level;
    private String price;
    private String duration;
    private String instructor;
    private Date created_at;
    private Date updated_at;
    @Enumerated(EnumType.STRING)
    private Status status;

    public Cours() {}
    public Cours(String title, String description, String cover, String video, String category, String language, LEVEL level, String price, String duration, String instructor, Date created_at, Date updated_at, Status status) {
        this.title = title;
        this.description = description;
        this.cover = cover;
        this.video = video;
        this.category = category;
        this.language = language;
        this.level = level;
        this.price = price;
        this.duration = duration;
        this.instructor = instructor;
        this.created_at = created_at;
        this.updated_at = updated_at;
        this.status = status;
    }
}
