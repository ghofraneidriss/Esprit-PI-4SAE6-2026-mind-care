package tn.esprit.souvenir_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.souvenir_service.dto.entree.EntreeCountResponse;
import tn.esprit.souvenir_service.dto.entree.EntreeSouvenirCreateRequest;
import tn.esprit.souvenir_service.dto.entree.EntreeSouvenirResponse;
import tn.esprit.souvenir_service.dto.entree.EntreeSouvenirUpdateRequest;
import tn.esprit.souvenir_service.dto.entree.VoiceRecognitionUpdateRequest;
import tn.esprit.souvenir_service.enums.MediaType;
import tn.esprit.souvenir_service.enums.ThemeCulturel;
import tn.esprit.souvenir_service.service.EntreeSouvenirService;

import java.util.List;

@RestController
@RequestMapping("/api/souvenirs")
@RequiredArgsConstructor
public class EntreeSouvenirController {

    private final EntreeSouvenirService entreeSouvenirService;

    @PostMapping
    public ResponseEntity<EntreeSouvenirResponse> create(@Valid @RequestBody EntreeSouvenirCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entreeSouvenirService.createEntree(request));
    }

    @GetMapping("/{entreeId}")
    public ResponseEntity<EntreeSouvenirResponse> getById(@PathVariable Long entreeId) {
        return ResponseEntity.ok(entreeSouvenirService.getEntreeById(entreeId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<EntreeSouvenirResponse>> getByPatient(
            @PathVariable Long patientId,
            @RequestParam(required = false) ThemeCulturel theme,
            @RequestParam(required = false) MediaType mediaType
    ) {
        return ResponseEntity.ok(entreeSouvenirService.getEntreesByPatient(patientId, theme, mediaType));
    }

    @GetMapping("/theme/{theme}")
    public ResponseEntity<List<EntreeSouvenirResponse>> getByTheme(@PathVariable ThemeCulturel theme) {
        return ResponseEntity.ok(entreeSouvenirService.getEntreesByTheme(theme));
    }

    @PutMapping("/{entreeId}")
    public ResponseEntity<EntreeSouvenirResponse> update(
            @PathVariable Long entreeId,
            @Valid @RequestBody EntreeSouvenirUpdateRequest request
    ) {
        return ResponseEntity.ok(entreeSouvenirService.updateEntree(entreeId, request));
    }

    @PatchMapping("/{entreeId}/traitee")
    public ResponseEntity<EntreeSouvenirResponse> markAsTraitee(@PathVariable Long entreeId) {
        return ResponseEntity.ok(entreeSouvenirService.markAsTraitee(entreeId));
    }

    @PatchMapping("/{entreeId}/voice-recognition")
    public ResponseEntity<EntreeSouvenirResponse> updateVoiceRecognition(
            @PathVariable Long entreeId,
            @Valid @RequestBody VoiceRecognitionUpdateRequest request
    ) {
        return ResponseEntity.ok(entreeSouvenirService.updateVoiceRecognition(entreeId, request));
    }

    @GetMapping("/patient/{patientId}/count")
    public ResponseEntity<EntreeCountResponse> countByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(entreeSouvenirService.countEntriesByPatient(patientId));
    }

    @DeleteMapping("/{entreeId}")
    public ResponseEntity<Void> delete(@PathVariable Long entreeId) {
        entreeSouvenirService.deleteEntree(entreeId);
        return ResponseEntity.noContent().build();
    }
}
