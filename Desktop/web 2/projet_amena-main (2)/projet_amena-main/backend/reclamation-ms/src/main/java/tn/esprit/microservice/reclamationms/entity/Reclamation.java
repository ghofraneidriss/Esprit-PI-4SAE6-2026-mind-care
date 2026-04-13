package tn.esprit.microservice.reclamationms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reclamations")
public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;
    private String userEmail;
    private String subject;
    private String imageUrl;
    private Integer rating;
    private String description;

    @Enumerated(EnumType.STRING)
    private ReclamationClassificationLevel classificationLevel;

    private Integer classificationScore;

    @Column(length = 500)
    private String classificationKeywords;

    @Column(length = 1000)
    private String classificationReason;

    private String classifiedBy;
    private LocalDateTime classifiedAt;

    @Enumerated(EnumType.STRING)
    private ReclamationStatus status;

    private LocalDateTime createdAt;

    public Reclamation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public ReclamationClassificationLevel getClassificationLevel() {
        return classificationLevel;
    }

    public void setClassificationLevel(ReclamationClassificationLevel classificationLevel) {
        this.classificationLevel = classificationLevel;
    }

    public Integer getClassificationScore() {
        return classificationScore;
    }

    public void setClassificationScore(Integer classificationScore) {
        this.classificationScore = classificationScore;
    }

    public String getClassificationKeywords() {
        return classificationKeywords;
    }

    public void setClassificationKeywords(String classificationKeywords) {
        this.classificationKeywords = classificationKeywords;
    }

    public String getClassificationReason() {
        return classificationReason;
    }

    public void setClassificationReason(String classificationReason) {
        this.classificationReason = classificationReason;
    }

    public String getClassifiedBy() {
        return classifiedBy;
    }

    public void setClassifiedBy(String classifiedBy) {
        this.classifiedBy = classifiedBy;
    }

    public LocalDateTime getClassifiedAt() {
        return classifiedAt;
    }

    public void setClassifiedAt(LocalDateTime classifiedAt) {
        this.classifiedAt = classifiedAt;
    }

    public ReclamationStatus getStatus() {
        return status;
    }

    public void setStatus(ReclamationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = ReclamationStatus.OPEN;
        }
        if (this.rating == null) {
            this.rating = 3;
        }
        if (this.classificationLevel == null) {
            this.classificationLevel = ReclamationClassificationLevel.MOYEN;
        }
        if (this.classificationScore == null) {
            this.classificationScore = 0;
        }
        if (this.classificationKeywords == null) {
            this.classificationKeywords = "";
        }
        if (this.classificationReason == null) {
            this.classificationReason = "NOT_CLASSIFIED";
        }
    }
}
