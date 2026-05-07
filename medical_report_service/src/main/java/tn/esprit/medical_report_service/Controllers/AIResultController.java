package tn.esprit.medical_report_service.Controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.medical_report_service.DTOs.AIResultDTO;
import tn.esprit.medical_report_service.Enteties.RiskLevel;
import tn.esprit.medical_report_service.Services.IAIResult;

@RestController
@RequestMapping("/api/ai-results")
@AllArgsConstructor
@CrossOrigin("*")
public class AIResultController {

    private final IAIResult aiResultService;

    @PostMapping
    public ResponseEntity<AIResultDTO> addAIResult(@Valid @RequestBody AIResultDTO aiResultDTO) {
        if (aiResultDTO.getMriScanId() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(aiResultService.addAIResult(aiResultDTO, aiResultDTO.getMriScanId()),
                HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AIResultDTO> updateAIResult(@PathVariable Long id,
            @Valid @RequestBody AIResultDTO aiResultDTO) {
        return new ResponseEntity<>(aiResultService.updateAIResult(id, aiResultDTO), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAIResult(@PathVariable Long id) {
        aiResultService.deleteAIResult(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AIResultDTO> getAIResultById(@PathVariable Long id) {
        return new ResponseEntity<>(aiResultService.getAIResultById(id), HttpStatus.OK);
    }

    @GetMapping(params = "riskLevel")
    public ResponseEntity<Page<AIResultDTO>> getAIResultsByRiskLevel(@RequestParam RiskLevel riskLevel,
            Pageable pageable) {
        return new ResponseEntity<>(aiResultService.getAIResultsByRiskLevel(riskLevel, pageable), HttpStatus.OK);
    }
}
