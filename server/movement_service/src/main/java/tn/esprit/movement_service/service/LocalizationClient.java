package tn.esprit.movement_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.movement_service.dto.SafeZoneDto;

import java.util.Collections;
import java.util.List;

@Service
public class LocalizationClient {

    private final RestTemplate restTemplate = new RestTemplate();

    /** Example: http://localhost:8085/api */
    @Value("${localization.service.base-url:http://localhost:8085/api}")
    private String localizationBaseUrl;

    public List<SafeZoneDto> getSafeZonesByPatientId(Long patientId) {
        try {
            String url = localizationBaseUrl + "/safezones/patient/" + patientId;
            ResponseEntity<List<SafeZoneDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<SafeZoneDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }

    public List<Long> getPatientIdsWithSafeZones() {
        try {
            String url = localizationBaseUrl + "/safezones/patient-ids";
            ResponseEntity<List<Long>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Long>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }
}
