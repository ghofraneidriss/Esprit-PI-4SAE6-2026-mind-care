package tn.esprit.traitement_et_consultation.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.traitement_et_consultation.client.dto.PredictionRequestDTO;
import tn.esprit.traitement_et_consultation.client.dto.PredictionResponseDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class MlPredictionClient {

    private final RestTemplate restTemplate;

    @Value("${ml.api.url:http://localhost:8000/predict}")
    private String mlApiUrl;

    public PredictionResponseDTO getAlzheimerPrediction(PredictionRequestDTO requestDTO) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<PredictionRequestDTO> request = new HttpEntity<>(requestDTO, headers);
            
            log.info("Sending prediction request to ML API: {}", requestDTO);
            ResponseEntity<PredictionResponseDTO> response = restTemplate.postForEntity(mlApiUrl, request, PredictionResponseDTO.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Received prediction response: {}", response.getBody());
                return response.getBody();
            } else {
                log.error("Failed to get a valid response from ML API. Status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error communicating with ML API: {}", e.getMessage());
            return null;
        }
    }
}
