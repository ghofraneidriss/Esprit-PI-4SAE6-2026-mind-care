package tn.esprit.lost_item_service.dto;

import tn.esprit.lost_item_service.Entity.LostItem;
import tn.esprit.lost_item_service.Entity.LostItemAlert;
import tn.esprit.lost_item_service.Entity.SearchReport;

import java.util.List;

/**
 * Utility class for converting between Entity and DTO objects.
 * Provides static methods to safely convert entities to their DTO representations.
 */
public class DTOMapper {

    private DTOMapper() {
        throw new IllegalStateException("Utility class");
    }

    // ── LostItem Mapping ──────────────────────────────────────────────────────

    /**
     * Convert LostItem entity to LostItemDTO (safe for API response)
     */
    public static LostItemDTO toLostItemDTO(LostItem entity) {
        if (entity == null) {
            return null;
        }
        return LostItemDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .patientId(entity.getPatientId())
                .caregiverId(entity.getCaregiverId())
                .lastSeenLocation(entity.getLastSeenLocation())
                .lastSeenDate(entity.getLastSeenDate())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .imageUrl(entity.getImageUrl())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Convert list of LostItem entities to LostItemDTOs
     */
    public static List<LostItemDTO> toLostItemDTOList(List<LostItem> entities) {
        return entities.stream()
                .map(DTOMapper::toLostItemDTO)
                .toList();
    }

    // ── LostItemAlert Mapping ─────────────────────────────────────────────────

    /**
     * Convert LostItemAlert entity to LostItemAlertDTO (safe for API response)
     */
    public static LostItemAlertDTO toLostItemAlertDTO(LostItemAlert entity) {
        if (entity == null) {
            return null;
        }
        return LostItemAlertDTO.builder()
                .id(entity.getId())
                .lostItemId(entity.getLostItemId())
                .patientId(entity.getPatientId())
                .caregiverId(entity.getCaregiverId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .level(entity.getLevel())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .viewedAt(entity.getViewedAt())
                .build();
    }

    /**
     * Convert list of LostItemAlert entities to LostItemAlertDTOs
     */
    public static List<LostItemAlertDTO> toLostItemAlertDTOList(List<LostItemAlert> entities) {
        return entities.stream()
                .map(DTOMapper::toLostItemAlertDTO)
                .toList();
    }

    // ── SearchReport Mapping ──────────────────────────────────────────────────

    /**
     * Convert SearchReport entity to SearchReportDTO (safe for API response)
     */
    public static SearchReportDTO toSearchReportDTO(SearchReport entity) {
        if (entity == null) {
            return null;
        }
        return SearchReportDTO.builder()
                .id(entity.getId())
                .lostItemId(entity.getLostItemId())
                .reportedBy(entity.getReportedBy())
                .searchDate(entity.getSearchDate())
                .locationSearched(entity.getLocationSearched())
                .searchResult(entity.getSearchResult())
                .notes(entity.getNotes())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Convert list of SearchReport entities to SearchReportDTOs
     */
    public static List<SearchReportDTO> toSearchReportDTOList(List<SearchReport> entities) {
        return entities.stream()
                .map(DTOMapper::toSearchReportDTO)
                .toList();
    }

    // ── Request DTO to Entity Mapping ─────────────────────────────────────────

    /**
     * Convert CreateLostItemRequest to LostItem entity
     */
    public static LostItem toLostItem(CreateLostItemRequest request) {
        if (request == null) {
            return null;
        }
        return LostItem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .patientId(request.getPatientId())
                .caregiverId(request.getCaregiverId())
                .lastSeenLocation(request.getLastSeenLocation())
                .lastSeenDate(request.getLastSeenDate())
                .priority(request.getPriority())
                .imageUrl(request.getImageUrl())
                .build();
    }

    /**
     * Convert CreateLostItemAlertRequest to LostItemAlert entity
     */
    public static LostItemAlert toLostItemAlert(CreateLostItemAlertRequest request) {
        if (request == null) {
            return null;
        }
        return LostItemAlert.builder()
                .lostItemId(request.getLostItemId())
                .patientId(request.getPatientId())
                .caregiverId(request.getCaregiverId())
                .title(request.getTitle())
                .description(request.getDescription())
                .level(request.getLevel())
                .build();
    }

    /**
     * Convert UpdateLostItemRequest to LostItem entity (for update operations)
     */
    public static LostItem toLostItemForUpdate(UpdateLostItemRequest request) {
        if (request == null) {
            return null;
        }
        return LostItem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .caregiverId(request.getCaregiverId())
                .lastSeenLocation(request.getLastSeenLocation())
                .lastSeenDate(request.getLastSeenDate())
                .status(request.getStatus())
                .priority(request.getPriority())
                .imageUrl(request.getImageUrl())
                .build();
    }

    /**
     * Convert UpdateLostItemAlertRequest to LostItemAlert entity (for update operations)
     */
    public static LostItemAlert toLostItemAlertForUpdate(UpdateLostItemAlertRequest request) {
        if (request == null) {
            return null;
        }
        return LostItemAlert.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .level(request.getLevel())
                .status(request.getStatus())
                .build();
    }

    /**
     * Convert CreateSearchReportRequest to SearchReport entity
     */
    public static SearchReport toSearchReport(CreateSearchReportRequest request) {
        if (request == null) {
            return null;
        }
        return SearchReport.builder()
                .lostItemId(request.getLostItemId())
                .reportedBy(request.getReportedBy())
                .searchDate(request.getSearchDate())
                .locationSearched(request.getLocationSearched())
                .searchResult(request.getSearchResult())
                .notes(request.getNotes())
                .build();
    }

    /**
     * Convert UpdateSearchReportRequest to SearchReport entity (for update operations)
     */
    public static SearchReport toSearchReportForUpdate(UpdateSearchReportRequest request) {
        if (request == null) {
            return null;
        }
        return SearchReport.builder()
                .searchDate(request.getSearchDate())
                .locationSearched(request.getLocationSearched())
                .searchResult(request.getSearchResult())
                .notes(request.getNotes())
                .status(request.getStatus())
                .build();
    }

}
