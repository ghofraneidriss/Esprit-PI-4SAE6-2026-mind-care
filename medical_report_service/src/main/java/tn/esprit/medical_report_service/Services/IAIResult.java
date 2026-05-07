package tn.esprit.medical_report_service.Services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.medical_report_service.DTOs.AIResultDTO;
import tn.esprit.medical_report_service.Enteties.RiskLevel;

public interface IAIResult {
    AIResultDTO addAIResult(AIResultDTO aiResultDTO, Long mriScanId);

    AIResultDTO updateAIResult(Long id, AIResultDTO aiResultDTO);

    void deleteAIResult(Long id);

    AIResultDTO getAIResultById(Long id);

    Page<AIResultDTO> getAIResultsByRiskLevel(RiskLevel riskLevel, Pageable pageable);
}
