package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.Entity.ReportStatus;
import tn.esprit.lost_item_service.Entity.SearchResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SearchReportDTOEdgeCasesTest {

    @Test
    void testEqualsWithSameObject() {
        SearchReportDTO dto = SearchReportDTO.builder().id(1L).lostItemId(10L).build();
        assertTrue(dto.equals(dto));
    }

    @Test
    void testEqualsWithNull() {
        SearchReportDTO dto = SearchReportDTO.builder().id(1L).lostItemId(10L).build();
        assertFalse(dto.equals(null));
    }

    @Test
    void testEqualsWithDifferentType() {
        SearchReportDTO dto = SearchReportDTO.builder().id(1L).lostItemId(10L).build();
        assertFalse(dto.equals("not a dto"));
    }

    @Test
    void testEqualsWithDifferentId() {
        SearchReportDTO dto1 = SearchReportDTO.builder().id(1L).lostItemId(10L).build();
        SearchReportDTO dto2 = SearchReportDTO.builder().id(2L).lostItemId(10L).build();
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentLostItemId() {
        SearchReportDTO dto1 = SearchReportDTO.builder().id(1L).lostItemId(10L).build();
        SearchReportDTO dto2 = SearchReportDTO.builder().id(1L).lostItemId(20L).build();
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentSearchResult() {
        SearchReportDTO dto1 = SearchReportDTO.builder().id(1L).searchResult(SearchResult.FOUND).build();
        SearchReportDTO dto2 = SearchReportDTO.builder().id(1L).searchResult(SearchResult.NOT_FOUND).build();
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentStatus() {
        SearchReportDTO dto1 = SearchReportDTO.builder().id(1L).status(ReportStatus.OPEN).build();
        SearchReportDTO dto2 = SearchReportDTO.builder().id(1L).status(ReportStatus.CLOSED).build();
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testHashCodeConsistency() {
        SearchReportDTO dto = SearchReportDTO.builder()
                .id(1L)
                .lostItemId(10L)
                .searchResult(SearchResult.FOUND)
                .build();
        int hash1 = dto.hashCode();
        int hash2 = dto.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashCodeWithEqualObjects() {
        SearchReportDTO dto1 = SearchReportDTO.builder().id(1L).lostItemId(10L).build();
        SearchReportDTO dto2 = SearchReportDTO.builder().id(1L).lostItemId(10L).build();
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToStringContainsFields() {
        SearchReportDTO dto = SearchReportDTO.builder()
                .id(5L)
                .lostItemId(10L)
                .locationSearched("Kitchen")
                .searchResult(SearchResult.FOUND)
                .build();

        String str = dto.toString();
        assertTrue(str.contains("5"));
        assertTrue(str.contains("10"));
        assertTrue(str.contains("Kitchen"));
        assertTrue(str.contains("FOUND"));
    }

    @Test
    void testAllSearchResultCombinations() {
        for (SearchResult result : SearchResult.values()) {
            SearchReportDTO dto = SearchReportDTO.builder().searchResult(result).build();
            assertEquals(result, dto.getSearchResult());
            assertNotNull(dto.toString());
        }
    }

    @Test
    void testAllReportStatusCombinations() {
        for (ReportStatus status : ReportStatus.values()) {
            SearchReportDTO dto = SearchReportDTO.builder().status(status).build();
            assertEquals(status, dto.getStatus());
            assertNotNull(dto.toString());
        }
    }

    @Test
    void testWithAllResultAndStatusCombinations() {
        for (SearchResult result : SearchResult.values()) {
            for (ReportStatus status : ReportStatus.values()) {
                SearchReportDTO dto = SearchReportDTO.builder()
                        .id(1L)
                        .lostItemId(10L)
                        .searchResult(result)
                        .status(status)
                        .build();

                assertEquals(result, dto.getSearchResult());
                assertEquals(status, dto.getStatus());
            }
        }
    }

    @Test
    void testWithDateValues() {
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.now().minusDays(5);

        SearchReportDTO dto1 = SearchReportDTO.builder().id(1L).searchDate(date1).build();
        SearchReportDTO dto2 = SearchReportDTO.builder().id(1L).searchDate(date2).build();

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testNullFieldsEquality() {
        SearchReportDTO dto1 = SearchReportDTO.builder()
                .id(1L)
                .locationSearched(null)
                .notes(null)
                .searchResult(null)
                .build();

        SearchReportDTO dto2 = SearchReportDTO.builder()
                .id(1L)
                .locationSearched(null)
                .notes(null)
                .searchResult(null)
                .build();

        assertEquals(dto1, dto2);
    }
}
