package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.Entity.AlertLevel;
import tn.esprit.lost_item_service.Entity.AlertStatus;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LostItemAlertDTOTest {

    @Test
    void testDefaultConstructor() {
        LostItemAlertDTO dto = new LostItemAlertDTO();
        assertNull(dto.getId());
        assertNull(dto.getTitle());
        assertNull(dto.getLevel());
        assertNull(dto.getStatus());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime created = LocalDateTime.now();
        LocalDateTime viewed = LocalDateTime.now();

        LostItemAlertDTO dto = new LostItemAlertDTO(
                1L, 10L, 5L, 3L, "Alert Title", "Alert Desc",
                AlertLevel.CRITICAL, AlertStatus.NEW, created, viewed
        );

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getLostItemId());
        assertEquals(5L, dto.getPatientId());
        assertEquals(3L, dto.getCaregiverId());
        assertEquals("Alert Title", dto.getTitle());
        assertEquals("Alert Desc", dto.getDescription());
        assertEquals(AlertLevel.CRITICAL, dto.getLevel());
        assertEquals(AlertStatus.NEW, dto.getStatus());
        assertEquals(created, dto.getCreatedAt());
        assertEquals(viewed, dto.getViewedAt());
    }

    @Test
    void testBuilderPattern() {
        LocalDateTime created = LocalDateTime.now();
        LocalDateTime viewed = LocalDateTime.now();

        LostItemAlertDTO dto = LostItemAlertDTO.builder()
                .id(2L)
                .lostItemId(20L)
                .patientId(15L)
                .caregiverId(8L)
                .title("Built Alert")
                .description("Built Description")
                .level(AlertLevel.HIGH)
                .status(AlertStatus.RESOLVED)
                .createdAt(created)
                .viewedAt(viewed)
                .build();

        assertEquals(2L, dto.getId());
        assertEquals(20L, dto.getLostItemId());
        assertEquals(15L, dto.getPatientId());
        assertEquals(8L, dto.getCaregiverId());
        assertEquals("Built Alert", dto.getTitle());
        assertEquals("Built Description", dto.getDescription());
        assertEquals(AlertLevel.HIGH, dto.getLevel());
        assertEquals(AlertStatus.RESOLVED, dto.getStatus());
        assertEquals(created, dto.getCreatedAt());
        assertEquals(viewed, dto.getViewedAt());
    }

    @Test
    void testSettersAndGetters() {
        LostItemAlertDTO dto = new LostItemAlertDTO();

        dto.setId(5L);
        dto.setLostItemId(50L);
        dto.setPatientId(25L);
        dto.setCaregiverId(10L);
        dto.setTitle("Setter Alert");
        dto.setDescription("Setter Description");
        dto.setLevel(AlertLevel.MEDIUM);
        dto.setStatus(AlertStatus.VIEWED);
        LocalDateTime created = LocalDateTime.now();
        dto.setCreatedAt(created);
        LocalDateTime viewed = LocalDateTime.now();
        dto.setViewedAt(viewed);

        assertEquals(5L, dto.getId());
        assertEquals(50L, dto.getLostItemId());
        assertEquals(25L, dto.getPatientId());
        assertEquals(10L, dto.getCaregiverId());
        assertEquals("Setter Alert", dto.getTitle());
        assertEquals("Setter Description", dto.getDescription());
        assertEquals(AlertLevel.MEDIUM, dto.getLevel());
        assertEquals(AlertStatus.VIEWED, dto.getStatus());
        assertEquals(created, dto.getCreatedAt());
        assertEquals(viewed, dto.getViewedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        LostItemAlertDTO dto1 = LostItemAlertDTO.builder().id(1L).title("Alert").level(AlertLevel.HIGH).build();
        LostItemAlertDTO dto2 = LostItemAlertDTO.builder().id(1L).title("Alert").level(AlertLevel.HIGH).build();
        LostItemAlertDTO dto3 = LostItemAlertDTO.builder().id(2L).title("Different").level(AlertLevel.LOW).build();

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, dto3);
        assertNotEquals(dto1.hashCode(), dto3.hashCode());
    }

    @Test
    void testToString() {
        LostItemAlertDTO dto = LostItemAlertDTO.builder().id(1L).title("Test").build();
        String str = dto.toString();

        assertNotNull(str);
        assertTrue(str.contains("LostItemAlertDTO"));
        assertTrue(str.contains("1"));
        assertTrue(str.contains("Test"));
    }

    @Test
    void testAllAlertLevels() {
        for (AlertLevel level : AlertLevel.values()) {
            LostItemAlertDTO dto = LostItemAlertDTO.builder().level(level).build();
            assertEquals(level, dto.getLevel());
        }
    }

    @Test
    void testAllAlertStatuses() {
        for (AlertStatus status : AlertStatus.values()) {
            LostItemAlertDTO dto = LostItemAlertDTO.builder().status(status).build();
            assertEquals(status, dto.getStatus());
        }
    }

    @Test
    void testPartialBuilding() {
        LostItemAlertDTO dto = LostItemAlertDTO.builder()
                .id(3L)
                .title("Partial Alert")
                .level(AlertLevel.CRITICAL)
                .build();

        assertEquals(3L, dto.getId());
        assertEquals("Partial Alert", dto.getTitle());
        assertEquals(AlertLevel.CRITICAL, dto.getLevel());
        assertNull(dto.getLostItemId());
        assertNull(dto.getPatientId());
        assertNull(dto.getStatus());
    }
}
