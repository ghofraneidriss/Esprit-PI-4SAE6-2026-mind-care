package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.entity.ReportStatus;
import tn.esprit.lost_item_service.entity.SearchResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SearchReportDTOTest {

    @Test
    void testDefaultConstructor() {
        SearchReportDTO dto = new SearchReportDTO();
        assertNull(dto.getId());
        assertNull(dto.getLostItemId());
        assertNull(dto.getSearchResult());
        assertNull(dto.getStatus());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate searchDate = LocalDate.now();
        LocalDateTime created = LocalDateTime.now();

        SearchReportDTO dto = new SearchReportDTO(
                1L, 10L, 5L, searchDate, "Living Room",
                SearchResult.FOUND, "Found in closet", ReportStatus.OPEN, created
        );

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getLostItemId());
        assertEquals(5L, dto.getReportedBy());
        assertEquals(searchDate, dto.getSearchDate());
        assertEquals("Living Room", dto.getLocationSearched());
        assertEquals(SearchResult.FOUND, dto.getSearchResult());
        assertEquals("Found in closet", dto.getNotes());
        assertEquals(ReportStatus.OPEN, dto.getStatus());
        assertEquals(created, dto.getCreatedAt());
    }

    @Test
    void testBuilderPattern() {
        LocalDate searchDate = LocalDate.now();
        LocalDateTime created = LocalDateTime.now();

        SearchReportDTO dto = SearchReportDTO.builder()
                .id(2L)
                .lostItemId(20L)
                .reportedBy(15L)
                .searchDate(searchDate)
                .locationSearched("Bedroom")
                .searchResult(SearchResult.NOT_FOUND)
                .notes("Not found anywhere")
                .status(ReportStatus.CLOSED)
                .createdAt(created)
                .build();

        assertEquals(2L, dto.getId());
        assertEquals(20L, dto.getLostItemId());
        assertEquals(15L, dto.getReportedBy());
        assertEquals(searchDate, dto.getSearchDate());
        assertEquals("Bedroom", dto.getLocationSearched());
        assertEquals(SearchResult.NOT_FOUND, dto.getSearchResult());
        assertEquals("Not found anywhere", dto.getNotes());
        assertEquals(ReportStatus.CLOSED, dto.getStatus());
        assertEquals(created, dto.getCreatedAt());
    }

    @Test
    void testSettersAndGetters() {
        SearchReportDTO dto = new SearchReportDTO();

        dto.setId(5L);
        dto.setLostItemId(50L);
        dto.setReportedBy(25L);
        LocalDate searchDate = LocalDate.now();
        dto.setSearchDate(searchDate);
        dto.setLocationSearched("Kitchen");
        dto.setSearchResult(SearchResult.PARTIALLY_FOUND);
        dto.setNotes("Partially found");
        dto.setStatus(ReportStatus.OPEN);
        LocalDateTime created = LocalDateTime.now();
        dto.setCreatedAt(created);

        assertEquals(5L, dto.getId());
        assertEquals(50L, dto.getLostItemId());
        assertEquals(25L, dto.getReportedBy());
        assertEquals(searchDate, dto.getSearchDate());
        assertEquals("Kitchen", dto.getLocationSearched());
        assertEquals(SearchResult.PARTIALLY_FOUND, dto.getSearchResult());
        assertEquals("Partially found", dto.getNotes());
        assertEquals(ReportStatus.OPEN, dto.getStatus());
        assertEquals(created, dto.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        SearchReportDTO dto1 = SearchReportDTO.builder().id(1L).lostItemId(10L).searchResult(SearchResult.FOUND).build();
        SearchReportDTO dto2 = SearchReportDTO.builder().id(1L).lostItemId(10L).searchResult(SearchResult.FOUND).build();
        SearchReportDTO dto3 = SearchReportDTO.builder().id(2L).lostItemId(20L).searchResult(SearchResult.NOT_FOUND).build();

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, dto3);
        assertNotEquals(dto1.hashCode(), dto3.hashCode());
    }

    @Test
    void testToString() {
        SearchReportDTO dto = SearchReportDTO.builder().id(1L).lostItemId(10L).build();
        String str = dto.toString();

        assertNotNull(str);
        assertTrue(str.contains("SearchReportDTO"));
        assertTrue(str.contains("1"));
        assertTrue(str.contains("10"));
    }

    @Test
    void testAllSearchResults() {
        for (SearchResult result : SearchResult.values()) {
            SearchReportDTO dto = SearchReportDTO.builder().searchResult(result).build();
            assertEquals(result, dto.getSearchResult());
        }
    }

    @Test
    void testAllReportStatuses() {
        for (ReportStatus status : ReportStatus.values()) {
            SearchReportDTO dto = SearchReportDTO.builder().status(status).build();
            assertEquals(status, dto.getStatus());
        }
    }

    @Test
    void testPartialBuilding() {
        LocalDate searchDate = LocalDate.now();

        SearchReportDTO dto = SearchReportDTO.builder()
                .id(3L)
                .lostItemId(30L)
                .locationSearched("Attic")
                .searchDate(searchDate)
                .build();

        assertEquals(3L, dto.getId());
        assertEquals(30L, dto.getLostItemId());
        assertEquals("Attic", dto.getLocationSearched());
        assertEquals(searchDate, dto.getSearchDate());
        assertNull(dto.getReportedBy());
        assertNull(dto.getSearchResult());
        assertNull(dto.getStatus());
    }

    @Test
    void testWithNullNotes() {
        SearchReportDTO dto = SearchReportDTO.builder()
                .id(1L)
                .lostItemId(10L)
                .notes(null)
                .build();

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getLostItemId());
        assertNull(dto.getNotes());
    }
}
