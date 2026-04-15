package tn.esprit.souvenir_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.souvenir_service.enums.MediaType;
import tn.esprit.souvenir_service.enums.ThemeCulturel;

import java.time.LocalDateTime;

@Entity
@Table(name = "entree_souvenirs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntreeSouvenir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column
    private Long doctorId;

    @Column
    private Long caregiverId;

    @Column(columnDefinition = "TEXT")
    private String infosCaregiver;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MediaType mediaType = MediaType.IMAGE;

    @Column(length = 500)
    private String mediaUrl;

    @Column(length = 255)
    private String mediaTitle;

    @Column(length = 255)
    private String expectedSpeakerName;

    @Column(length = 255)
    private String expectedSpeakerRelation;

    @Column(length = 255)
    private String patientGuessSpeakerName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean voiceRecognized = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ThemeCulturel themeCulturel;

    @Column(nullable = false)
    @Builder.Default
    private Boolean traitee = Boolean.FALSE;

    @Column(nullable = false)
    @Builder.Default
    private Integer importance = 5;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
