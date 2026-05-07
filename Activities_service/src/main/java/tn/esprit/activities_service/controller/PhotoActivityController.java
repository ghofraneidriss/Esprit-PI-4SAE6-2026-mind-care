package tn.esprit.activities_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.activities_service.entity.PhotoActivity;
import tn.esprit.activities_service.service.PhotoActivityService;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/photo-activities")
@Tag(name = "Photo Activities Management", description = "API pour la gestion des activités photo")
public class PhotoActivityController {

    @Autowired
    private PhotoActivityService photoActivityService;

    @Operation(summary = "Récupérer toutes les activités photo")
    @GetMapping
    public ResponseEntity<List<PhotoActivity>> getAllPhotoActivities() {
        List<PhotoActivity> photos = photoActivityService.getAllPhotoActivities();
        return ResponseEntity.ok(photos);
    }

    @Operation(summary = "Image binaire (JPEG stocké en base) ou redirection URL externe")
    @GetMapping("/{id}/image")
    public ResponseEntity<?> getPhotoImage(@PathVariable Long id) {
        Optional<byte[]> bytes = photoActivityService.getImageBytes(id);
        if (bytes.isPresent()) {
            String ct = photoActivityService.getImageContentType(id).orElse("image/jpeg");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(ct))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(bytes.get());
        }
        Optional<String> external = photoActivityService.getExternalImageUrl(id);
        if (external.isPresent() && !external.get().isBlank()) {
            String u = external.get();
            if (u.startsWith("http://") || u.startsWith("https://")) {
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(u)).build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Récupérer une activité photo par ID")
    @GetMapping("/{id}")
    public ResponseEntity<PhotoActivity> getPhotoActivityById(
            @Parameter(description = "ID") @PathVariable Long id) {
        Optional<PhotoActivity> photo = photoActivityService.getPhotoActivityById(id);
        return photo.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Création JSON désactivée — utiliser POST /with-image (fichier uniquement)")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createPhotoActivityJsonDisabled() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message",
                "Image upload is required. Use POST /api/photo-activities/with-image with a multipart file."));
    }

    @Operation(summary = "Créer avec fichier image (redimensionné / JPEG en base)")
    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createWithImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "EASY") String difficulty,
            @RequestParam String correctAnswer,
            @RequestParam String optionsJson
    ) {
        try {
            PhotoActivity created = photoActivityService.createWithImage(
                    file, title, description, difficulty, correctAnswer, optionsJson);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage() != null ? e.getMessage() : "Invalid image"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Could not read the image file. Please try another JPEG or PNG."));
        }
    }

    @Operation(summary = "Mettre à jour (JSON)")
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PhotoActivity> updatePhotoActivity(
            @PathVariable Long id,
            @RequestBody PhotoActivity photoActivity) {
        PhotoActivity updatedPhoto = photoActivityService.updatePhotoActivity(id, photoActivity);
        if (updatedPhoto != null) {
            return ResponseEntity.ok(updatedPhoto);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Mettre à jour avec fichier image optionnel")
    @PutMapping(value = "/{id}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateWithImage(
            @PathVariable Long id,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "EASY") String difficulty,
            @RequestParam String correctAnswer,
            @RequestParam String optionsJson
    ) {
        try {
            PhotoActivity updated = photoActivityService.updateWithImage(
                    id, file, title, description, difficulty, correctAnswer, optionsJson);
            if (updated != null) {
                return ResponseEntity.ok(updated);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage() != null ? e.getMessage() : "Invalid image"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Could not read the image file. Please try another JPEG or PNG."));
        }
    }

    @Operation(summary = "Supprimer")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePhotoActivity(@PathVariable Long id) {
        boolean deleted = photoActivityService.deletePhotoActivity(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Par difficulté")
    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<PhotoActivity>> getPhotoActivitiesByDifficulty(
            @PathVariable String difficulty) {
        List<PhotoActivity> photos = photoActivityService.getPhotoActivitiesByDifficulty(difficulty);
        return ResponseEntity.ok(photos);
    }

    @Operation(summary = "Recherche par titre")
    @GetMapping("/search")
    public ResponseEntity<List<PhotoActivity>> searchPhotoActivities(@RequestParam String title) {
        List<PhotoActivity> photos = photoActivityService.searchPhotoActivities(title);
        return ResponseEntity.ok(photos);
    }
}
