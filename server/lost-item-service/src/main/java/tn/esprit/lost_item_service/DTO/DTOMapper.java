package tn.esprit.lost_item_service.DTO;

import tn.esprit.lost_item_service.Entity.LostItem;
import tn.esprit.lost_item_service.Entity.LostItemAlert;
import tn.esprit.lost_item_service.Entity.SearchReport;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for converting between Entity and DTO objects.
 * Provides static methods to safely convert entities to their DTO representations.
 */
public class DTOMapper {

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
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
    }

}
