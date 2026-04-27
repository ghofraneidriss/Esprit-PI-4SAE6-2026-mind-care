package tn.esprit.activities_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "photo_activity")
@Data
@NoArgsConstructor
@ToString(exclude = "imageData")
@EqualsAndHashCode(exclude = "imageData")
public class PhotoActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * URL externe (legacy) — ignorée en JSON ; utiliser getImageUrl() pour l’API.
     */
    @Column(name = "image_url", nullable = true)
    @JsonIgnore
    private String externalImageUrl;

    /**
     * Image stockée en base (JPEG compressé). Jamais sérialisée en JSON.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image_data", columnDefinition = "LONGBLOB")
    @JsonIgnore
    private byte[] imageData;

    @Column(name = "image_content_type", length = 64)
    @JsonIgnore
    private String imageContentType = "image/jpeg";

    /**
     * Indique si l’affichage doit passer par GET /api/photo-activities/{id}/image
     */
    @Column(name = "has_stored_image", nullable = false)
    private boolean hasStoredImage = false;

    @Column(nullable = false)
    private String difficulty;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Convert(converter = StringListConverter.class)
    @Column(name = "options", columnDefinition = "TEXT")
    private List<String> options = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private java.util.Date createdAt;

    @Column(name = "updated_at")
    private java.util.Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.util.Date();
        updatedAt = new java.util.Date();
        normalizeStoredImageUrlColumn();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.util.Date();
        normalizeStoredImageUrlColumn();
    }

    /**
     * MySQL {@code image_url} is often NOT NULL; when the binary is in {@code image_data}, persist "" not null.
     */
    private void normalizeStoredImageUrlColumn() {
        if (hasStoredImage && (externalImageUrl == null || externalImageUrl.isBlank())) {
            externalImageUrl = "";
        }
    }

    @JsonProperty("imageUrl")
    public String getImageUrl() {
        if (hasStoredImage && id != null) {
            return "/api/photo-activities/" + id + "/image";
        }
        if (externalImageUrl != null && !externalImageUrl.isBlank()) {
            return externalImageUrl;
        }
        return "";
    }

    @JsonProperty("imageUrl")
    public void setImageUrl(String url) {
        this.externalImageUrl = url;
    }
}
