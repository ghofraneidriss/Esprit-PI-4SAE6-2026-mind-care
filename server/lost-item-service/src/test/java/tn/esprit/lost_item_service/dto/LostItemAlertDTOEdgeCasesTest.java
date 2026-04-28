package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.Entity.AlertLevel;
import tn.esprit.lost_item_service.Entity.AlertStatus;

import static org.junit.jupiter.api.Assertions.*;

class LostItemAlertDTOEdgeCasesTest {

    @Test
    void testEqualsWithSameObject() {
        LostItemAlertDTO dto = LostItemAlertDTO.builder().id(1L).title("Alert").build();
        assertTrue(dto.equals(dto));
    }

    @Test
    void testEqualsWithNull() {
        LostItemAlertDTO dto = LostItemAlertDTO.builder().id(1L).title("Alert").build();
        assertFalse(dto.equals(null));
    }

    @Test
    void testEqualsWithDifferentType() {
        LostItemAlertDTO dto = LostItemAlertDTO.builder().id(1L).title("Alert").build();
        assertFalse(dto.equals("not a dto"));
    }

    @Test
    void testEqualsWithDifferentId() {
        LostItemAlertDTO dto1 = LostItemAlertDTO.builder().id(1L).title("Alert").build();
        LostItemAlertDTO dto2 = LostItemAlertDTO.builder().id(2L).title("Alert").build();
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentLevel() {
        LostItemAlertDTO dto1 = LostItemAlertDTO.builder().id(1L).level(AlertLevel.HIGH).build();
        LostItemAlertDTO dto2 = LostItemAlertDTO.builder().id(1L).level(AlertLevel.LOW).build();
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentStatus() {
        LostItemAlertDTO dto1 = LostItemAlertDTO.builder().id(1L).status(AlertStatus.NEW).build();
        LostItemAlertDTO dto2 = LostItemAlertDTO.builder().id(1L).status(AlertStatus.RESOLVED).build();
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithNullFields() {
        LostItemAlertDTO dto1 = LostItemAlertDTO.builder().id(1L).title(null).level(null).build();
        LostItemAlertDTO dto2 = LostItemAlertDTO.builder().id(1L).title(null).level(null).build();
        assertEquals(dto1, dto2);
    }

    @Test
    void testHashCodeConsistency() {
        LostItemAlertDTO dto = LostItemAlertDTO.builder().id(1L).title("Alert").level(AlertLevel.CRITICAL).build();
        int hash1 = dto.hashCode();
        int hash2 = dto.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashCodeWithEqualObjects() {
        LostItemAlertDTO dto1 = LostItemAlertDTO.builder().id(1L).title("Alert").build();
        LostItemAlertDTO dto2 = LostItemAlertDTO.builder().id(1L).title("Alert").build();
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToStringContainsFields() {
        LostItemAlertDTO dto = LostItemAlertDTO.builder()
                .id(5L)
                .title("MyAlert")
                .level(AlertLevel.CRITICAL)
                .build();

        String str = dto.toString();
        assertTrue(str.contains("5"));
        assertTrue(str.contains("MyAlert"));
        assertTrue(str.contains("CRITICAL"));
    }

    @Test
    void testAllAlertLevelCombinations() {
        for (AlertLevel level : AlertLevel.values()) {
            LostItemAlertDTO dto = LostItemAlertDTO.builder().level(level).build();
            assertEquals(level, dto.getLevel());
            assertNotNull(dto.toString());
        }
    }

    @Test
    void testAllAlertStatusCombinations() {
        for (AlertStatus status : AlertStatus.values()) {
            LostItemAlertDTO dto = LostItemAlertDTO.builder().status(status).build();
            assertEquals(status, dto.getStatus());
            assertNotNull(dto.toString());
        }
    }

    @Test
    void testWithAllCombinations() {
        for (AlertLevel level : AlertLevel.values()) {
            for (AlertStatus status : AlertStatus.values()) {
                LostItemAlertDTO dto = LostItemAlertDTO.builder()
                        .id(1L)
                        .title("Test")
                        .level(level)
                        .status(status)
                        .build();

                assertEquals(level, dto.getLevel());
                assertEquals(status, dto.getStatus());
            }
        }
    }

    @Test
    void testHashCodeWithAllLevels() {
        for (AlertLevel level : AlertLevel.values()) {
            LostItemAlertDTO dto1 = LostItemAlertDTO.builder().id(1L).level(level).build();
            LostItemAlertDTO dto2 = LostItemAlertDTO.builder().id(1L).level(level).build();
            assertEquals(dto1.hashCode(), dto2.hashCode());
        }
    }
}
