package tn.esprit.medical_report_service.Services;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.esprit.medical_report_service.DTOs.AIResultDTO;
import tn.esprit.medical_report_service.Enteties.AIResult;
import tn.esprit.medical_report_service.Enteties.MRIScan;
import tn.esprit.medical_report_service.Enteties.RiskLevel;
import tn.esprit.medical_report_service.Exceptions.ResourceNotFoundException;
import tn.esprit.medical_report_service.Mappers.AIResultMapper;
import tn.esprit.medical_report_service.Repositories.AIResultRepository;
import tn.esprit.medical_report_service.Repositories.MRIScanRepository;

@Service
@AllArgsConstructor
public class AIResultService implements IAIResult {

    private final AIResultRepository aiResultRepository;
    private final MRIScanRepository mriScanRepository;
    private final AIResultMapper aiResultMapper;

    @Override
    public AIResultDTO addAIResult(AIResultDTO aiResultDTO, Long mriScanId) {
        MRIScan scan = mriScanRepository.findById(mriScanId)
                .orElseThrow(() -> new ResourceNotFoundException("MRIScan not found with id: " + mriScanId));

        AIResult aiResult = aiResultMapper.toEntity(aiResultDTO);
        aiResult.setMriScan(scan);
        aiResult = aiResultRepository.save(aiResult);
        return aiResultMapper.toDTO(aiResult);
    }

    @Override
    public AIResultDTO updateAIResult(Long id, AIResultDTO aiResultDTO) {
        AIResult existingResult = aiResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AIResult not found with id: " + id));

        aiResultDTO.setId(id);
        AIResult aiResult = aiResultMapper.toEntity(aiResultDTO);

        // Preserve scan link
        aiResult.setMriScan(existingResult.getMriScan());

        aiResult = aiResultRepository.save(aiResult);
        return aiResultMapper.toDTO(aiResult);
    }

    @Override
    public void deleteAIResult(Long id) {
        AIResult aiResult = aiResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AIResult not found with id: " + id));
        aiResultRepository.delete(aiResult);
    }

    @Override
    public AIResultDTO getAIResultById(Long id) {
        AIResult aiResult = aiResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AIResult not found with id: " + id));
        return aiResultMapper.toDTO(aiResult);
    }

    @Override
    public Page<AIResultDTO> getAIResultsByRiskLevel(RiskLevel riskLevel, Pageable pageable) {
        return aiResultRepository.findByRiskLevel(riskLevel, pageable)
                .map(aiResultMapper::toDTO);
    }
}
