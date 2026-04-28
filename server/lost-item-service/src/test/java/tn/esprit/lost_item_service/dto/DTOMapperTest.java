package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.Entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DTOMapperTest {

    // ── LostItem Mapping Tests ────────────────────────────────────────────────

    @Test
    void testToLostItemDTO_withValidEntity() {
        LostItem entity = LostItem.builder()
                .id(1L)
                .title("Test Item")
                .description("Test Description")
                .category(ItemCategory.MEDICATION)
                .patientId(10L)
                .caregiverId(5L)
                .lastSeenLocation("Kitchen")
                .lastSeenDate(LocalDate.now())
                .status(ItemStatus.LOST)
                .priority(ItemPriority.CRITICAL)
                .imageUrl("http://image.url")
                .createdAt(LocalDateTime.now())
                .build();

        LostItemDTO dto = DTOMapper.toLostItemDTO(entity);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Test Item", dto.getTitle());
        assertEquals("Test Description", dto.getDescription());
        assertEquals(ItemCategory.MEDICATION, dto.getCategory());
        assertEquals(10L, dto.getPatientId());
        assertEquals(5L, dto.getCaregiverId());
        assertEquals("Kitchen", dto.getLastSeenLocation());
        assertEquals(ItemStatus.LOST, dto.getStatus());
        assertEquals(ItemPriority.CRITICAL, dto.getPriority());
        assertEquals("http://image.url", dto.getImageUrl());
    }

    @Test
    void testToLostItemDTO_withNullEntity() {
        LostItemDTO dto = DTOMapper.toLostItemDTO(null);
        assertNull(dto);
    }

    @Test
    void testToLostItemDTOList_withMultipleEntities() {
        LostItem entity1 = LostItem.builder().id(1L).title("Item 1").category(ItemCategory.CLOTHING).patientId(1L).build();
        LostItem entity2 = LostItem.builder().id(2L).title("Item 2").category(ItemCategory.DOCUMENT).patientId(2L).build();
        List<LostItem> entities = Arrays.asList(entity1, entity2);

        List<LostItemDTO> dtos = DTOMapper.toLostItemDTOList(entities);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
        assertEquals("Item 1", dtos.get(0).getTitle());
        assertEquals(2L, dtos.get(1).getId());
        assertEquals("Item 2", dtos.get(1).getTitle());
    }

    @Test
    void testToLostItemDTOList_withEmptyList() {
        List<LostItemDTO> dtos = DTOMapper.toLostItemDTOList(List.of());
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    // ── LostItemAlert Mapping Tests ───────────────────────────────────────────

    @Test
    void testToLostItemAlertDTO_withValidEntity() {
        LostItemAlert entity = LostItemAlert.builder()
                .id(1L)
                .lostItemId(10L)
                .patientId(5L)
                .caregiverId(3L)
                .title("Alert Title")
                .description("Alert Description")
                .level(AlertLevel.CRITICAL)
                .status(AlertStatus.NEW)
                .createdAt(LocalDateTime.now())
                .viewedAt(LocalDateTime.now())
                .build();

        LostItemAlertDTO dto = DTOMapper.toLostItemAlertDTO(entity);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getLostItemId());
        assertEquals(5L, dto.getPatientId());
        assertEquals(3L, dto.getCaregiverId());
        assertEquals("Alert Title", dto.getTitle());
        assertEquals("Alert Description", dto.getDescription());
        assertEquals(AlertLevel.CRITICAL, dto.getLevel());
        assertEquals(AlertStatus.NEW, dto.getStatus());
    }

    @Test
    void testToLostItemAlertDTO_withNullEntity() {
        LostItemAlertDTO dto = DTOMapper.toLostItemAlertDTO(null);
        assertNull(dto);
    }

    @Test
    void testToLostItemAlertDTOList_withMultipleEntities() {
        LostItemAlert entity1 = LostItemAlert.builder().id(1L).title("Alert 1").level(AlertLevel.HIGH).build();
        LostItemAlert entity2 = LostItemAlert.builder().id(2L).title("Alert 2").level(AlertLevel.LOW).build();
        List<LostItemAlert> entities = Arrays.asList(entity1, entity2);

        List<LostItemAlertDTO> dtos = DTOMapper.toLostItemAlertDTOList(entities);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
        assertEquals("Alert 1", dtos.get(0).getTitle());
        assertEquals(AlertLevel.HIGH, dtos.get(0).getLevel());
        assertEquals(2L, dtos.get(1).getId());
        assertEquals("Alert 2", dtos.get(1).getTitle());
        assertEquals(AlertLevel.LOW, dtos.get(1).getLevel());
    }

    @Test
    void testToLostItemAlertDTOList_withEmptyList() {
        List<LostItemAlertDTO> dtos = DTOMapper.toLostItemAlertDTOList(List.of());
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    // ── SearchReport Mapping Tests ────────────────────────────────────────────

    @Test
    void testToSearchReportDTO_withValidEntity() {
        SearchReport entity = SearchReport.builder()
                .id(1L)
                .lostItemId(10L)
                .reportedBy(5L)
                .searchDate(LocalDate.now())
                .locationSearched("Living Room")
                .searchResult(SearchResult.FOUND)
                .notes("Found in closet")
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();

        SearchReportDTO dto = DTOMapper.toSearchReportDTO(entity);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getLostItemId());
        assertEquals(5L, dto.getReportedBy());
        assertEquals("Living Room", dto.getLocationSearched());
        assertEquals(SearchResult.FOUND, dto.getSearchResult());
        assertEquals("Found in closet", dto.getNotes());
        assertEquals(ReportStatus.OPEN, dto.getStatus());
    }

    @Test
    void testToSearchReportDTO_withNullEntity() {
        SearchReportDTO dto = DTOMapper.toSearchReportDTO(null);
        assertNull(dto);
    }

    @Test
    void testToSearchReportDTOList_withMultipleEntities() {
        SearchReport entity1 = SearchReport.builder().id(1L).lostItemId(10L).searchResult(SearchResult.NOT_FOUND).build();
        SearchReport entity2 = SearchReport.builder().id(2L).lostItemId(20L).searchResult(SearchResult.PARTIALLY_FOUND).build();
        List<SearchReport> entities = Arrays.asList(entity1, entity2);

        List<SearchReportDTO> dtos = DTOMapper.toSearchReportDTOList(entities);

        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
        assertEquals(10L, dtos.get(0).getLostItemId());
        assertEquals(SearchResult.NOT_FOUND, dtos.get(0).getSearchResult());
        assertEquals(2L, dtos.get(1).getId());
        assertEquals(20L, dtos.get(1).getLostItemId());
        assertEquals(SearchResult.PARTIALLY_FOUND, dtos.get(1).getSearchResult());
    }

    @Test
    void testToSearchReportDTOList_withEmptyList() {
        List<SearchReportDTO> dtos = DTOMapper.toSearchReportDTOList(List.of());
        assertNotNull(dtos);
        assertTrue(dtos.isEmpty());
    }

    // ── Request DTO to Entity Mapping Tests ───────────────────────────────────

    @Test
    void testToLostItem_withValidRequest() {
        CreateLostItemRequest request = CreateLostItemRequest.builder()
                .title("New Lost Item")
                .description("Description")
                .category(ItemCategory.DOCUMENT)
                .patientId(5L)
                .caregiverId(2L)
                .lastSeenLocation("Bedroom")
                .lastSeenDate(LocalDate.now())
                .priority(ItemPriority.MEDIUM)
                .imageUrl("http://image.url")
                .build();

        LostItem entity = DTOMapper.toLostItem(request);

        assertNotNull(entity);
        assertEquals("New Lost Item", entity.getTitle());
        assertEquals("Description", entity.getDescription());
        assertEquals(ItemCategory.DOCUMENT, entity.getCategory());
        assertEquals(5L, entity.getPatientId());
        assertEquals(2L, entity.getCaregiverId());
        assertEquals("Bedroom", entity.getLastSeenLocation());
        assertEquals(ItemPriority.MEDIUM, entity.getPriority());
        assertEquals("http://image.url", entity.getImageUrl());
    }

    @Test
    void testToLostItem_withNullRequest() {
        LostItem entity = DTOMapper.toLostItem(null);
        assertNull(entity);
    }

    @Test
    void testToLostItem_withMinimalRequest() {
        CreateLostItemRequest request = CreateLostItemRequest.builder()
                .title("Minimal Item")
                .category(ItemCategory.ACCESSORY)
                .patientId(3L)
                .build();

        LostItem entity = DTOMapper.toLostItem(request);

        assertNotNull(entity);
        assertEquals("Minimal Item", entity.getTitle());
        assertEquals(ItemCategory.ACCESSORY, entity.getCategory());
        assertEquals(3L, entity.getPatientId());
        assertNull(entity.getCaregiverId());
        assertNull(entity.getDescription());
    }

    @Test
    void testToLostItemAlert_withValidRequest() {
        CreateLostItemAlertRequest request = CreateLostItemAlertRequest.builder()
                .lostItemId(10L)
                .patientId(5L)
                .caregiverId(2L)
                .title("New Alert")
                .description("Alert Description")
                .level(AlertLevel.CRITICAL)
                .build();

        LostItemAlert entity = DTOMapper.toLostItemAlert(request);

        assertNotNull(entity);
        assertEquals(10L, entity.getLostItemId());
        assertEquals(5L, entity.getPatientId());
        assertEquals(2L, entity.getCaregiverId());
        assertEquals("New Alert", entity.getTitle());
        assertEquals("Alert Description", entity.getDescription());
        assertEquals(AlertLevel.CRITICAL, entity.getLevel());
    }

    @Test
    void testToLostItemAlert_withNullRequest() {
        LostItemAlert entity = DTOMapper.toLostItemAlert(null);
        assertNull(entity);
    }

    @Test
    void testToLostItemAlert_withMinimalRequest() {
        CreateLostItemAlertRequest request = CreateLostItemAlertRequest.builder()
                .lostItemId(10L)
                .patientId(5L)
                .title("Minimal Alert")
                .level(AlertLevel.LOW)
                .build();

        LostItemAlert entity = DTOMapper.toLostItemAlert(request);

        assertNotNull(entity);
        assertEquals(10L, entity.getLostItemId());
        assertEquals(5L, entity.getPatientId());
        assertEquals("Minimal Alert", entity.getTitle());
        assertEquals(AlertLevel.LOW, entity.getLevel());
        assertNull(entity.getCaregiverId());
        assertNull(entity.getDescription());
    }

    // ── Constructor Test ──────────────────────────────────────────────────────

    @Test
    void testPrivateConstructor_isPrivate() throws Exception {
        var constructor = DTOMapper.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        var invocationException = assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(IllegalStateException.class, invocationException.getCause());
        assertEquals("Utility class", invocationException.getCause().getMessage());
    }
}
