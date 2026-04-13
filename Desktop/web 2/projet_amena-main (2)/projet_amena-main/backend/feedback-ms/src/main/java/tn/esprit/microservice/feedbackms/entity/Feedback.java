package tn.esprit.microservice.feedbackms.entity;

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
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;
    private String userEmail;
    private String title;
    private String imageUrl;
    private Integer rating;
    private String comment;

    @Enumerated(EnumType.STRING)
    private FeedbackModerationStatus moderationStatus;

    private Integer moderationScore;
    private Boolean moderationFlagged;

    @Column(length = 500)
    private String blockedWords;

    @Column(length = 1000)
    private String moderationNote;

    private String moderatedBy;
    private LocalDateTime moderatedAt;

    private Boolean autoReclamationCreated;
    private Long linkedReclamationId;

    @Column(length = 255)
    private String autoReclamationStatus;

    private LocalDateTime createdAt;

    public Feedback() {
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public FeedbackModerationStatus getModerationStatus() {
        return moderationStatus;
    }

    public void setModerationStatus(FeedbackModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }

    public Integer getModerationScore() {
        return moderationScore;
    }

    public void setModerationScore(Integer moderationScore) {
        this.moderationScore = moderationScore;
    }

    public Boolean getModerationFlagged() {
        return moderationFlagged;
    }

    public void setModerationFlagged(Boolean moderationFlagged) {
        this.moderationFlagged = moderationFlagged;
    }

    public String getBlockedWords() {
        return blockedWords;
    }

    public void setBlockedWords(String blockedWords) {
        this.blockedWords = blockedWords;
    }

    public String getModerationNote() {
        return moderationNote;
    }

    public void setModerationNote(String moderationNote) {
        this.moderationNote = moderationNote;
    }

    public String getModeratedBy() {
        return moderatedBy;
    }

    public void setModeratedBy(String moderatedBy) {
        this.moderatedBy = moderatedBy;
    }

    public LocalDateTime getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(LocalDateTime moderatedAt) {
        this.moderatedAt = moderatedAt;
    }

    public Boolean getAutoReclamationCreated() {
        return autoReclamationCreated;
    }

    public void setAutoReclamationCreated(Boolean autoReclamationCreated) {
        this.autoReclamationCreated = autoReclamationCreated;
    }

    public Long getLinkedReclamationId() {
        return linkedReclamationId;
    }

    public void setLinkedReclamationId(Long linkedReclamationId) {
        this.linkedReclamationId = linkedReclamationId;
    }

    public String getAutoReclamationStatus() {
        return autoReclamationStatus;
    }

    public void setAutoReclamationStatus(String autoReclamationStatus) {
        this.autoReclamationStatus = autoReclamationStatus;
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
        if (this.moderationStatus == null) {
            this.moderationStatus = FeedbackModerationStatus.PENDING;
        }
        if (this.moderationFlagged == null) {
            this.moderationFlagged = false;
        }
        if (this.autoReclamationCreated == null) {
            this.autoReclamationCreated = false;
        }
        if (this.autoReclamationStatus == null) {
            this.autoReclamationStatus = "NOT_EVALUATED";
        }
    }
}
