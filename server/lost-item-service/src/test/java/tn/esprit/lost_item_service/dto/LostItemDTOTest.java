package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.entity.ItemCategory;
import tn.esprit.lost_item_service.entity.ItemPriority;
import tn.esprit.lost_item_service.entity.ItemStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LostItemDTOTest {

    @Test
    void testDefaultConstructor() {
        LostItemDTO dto = new LostItemDTO();
        assertNull(dto.getId());
        assertNull(dto.getTitle());
        assertNull(dto.getCategory());
        assertNull(dto.getStatus());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate lastSeen = LocalDate.now();
        LocalDateTime created = LocalDateTime.now();

        LostItemDTO dto = new LostItemDTO(
                1L, "Test Item", "Test Description", ItemCategory.MEDICATION,
                10L, 5L, "Kitchen", lastSeen, ItemStatus.LOST,
                ItemPriority.CRITICAL, "http://image.url", created
        );

        assertEquals(1L, dto.getId());
        assertEquals("Test Item", dto.getTitle());
        assertEquals("Test Description", dto.getDescription());
        assertEquals(ItemCategory.MEDICATION, dto.getCategory());
        assertEquals(10L, dto.getPatientId());
        assertEquals(5L, dto.getCaregiverId());
        assertEquals("Kitchen", dto.getLastSeenLocation());
        assertEquals(lastSeen, dto.getLastSeenDate());
        assertEquals(ItemStatus.LOST, dto.getStatus());
        assertEquals(ItemPriority.CRITICAL, dto.getPriority());
        assertEquals("http://image.url", dto.getImageUrl());
        assertEquals(created, dto.getCreatedAt());
    }

    @Test
    void testBuilderPattern() {
        LocalDate lastSeen = LocalDate.now();
        LocalDateTime created = LocalDateTime.now();

        LostItemDTO dto = LostItemDTO.builder()
                .id(2L)
                .title("Built Item")
                .description("Built Description")
                .category(ItemCategory.CLOTHING)
                .patientId(20L)
                .caregiverId(15L)
                .lastSeenLocation("Bedroom")
                .lastSeenDate(lastSeen)
                .status(ItemStatus.FOUND)
                .priority(ItemPriority.LOW)
                .imageUrl("http://built.url")
                .createdAt(created)
                .build();

        assertEquals(2L, dto.getId());
        assertEquals("Built Item", dto.getTitle());
        assertEquals("Built Description", dto.getDescription());
        assertEquals(ItemCategory.CLOTHING, dto.getCategory());
        assertEquals(20L, dto.getPatientId());
        assertEquals(15L, dto.getCaregiverId());
        assertEquals("Bedroom", dto.getLastSeenLocation());
        assertEquals(lastSeen, dto.getLastSeenDate());
        assertEquals(ItemStatus.FOUND, dto.getStatus());
        assertEquals(ItemPriority.LOW, dto.getPriority());
        assertEquals("http://built.url", dto.getImageUrl());
        assertEquals(created, dto.getCreatedAt());
    }

    @Test
    void testSettersAndGetters() {
        LostItemDTO dto = new LostItemDTO();

        dto.setId(5L);
        dto.setTitle("Setter Item");
        dto.setDescription("Setter Description");
        dto.setCategory(ItemCategory.DOCUMENT);
        dto.setPatientId(50L);
        dto.setCaregiverId(25L);
        dto.setLastSeenLocation("Living Room");
        LocalDate lastSeen = LocalDate.now();
        dto.setLastSeenDate(lastSeen);
        dto.setStatus(ItemStatus.LOST);
        dto.setPriority(ItemPriority.MEDIUM);
        dto.setImageUrl("http://setter.url");
        LocalDateTime created = LocalDateTime.now();
        dto.setCreatedAt(created);

        assertEquals(5L, dto.getId());
        assertEquals("Setter Item", dto.getTitle());
        assertEquals("Setter Description", dto.getDescription());
        assertEquals(ItemCategory.DOCUMENT, dto.getCategory());
        assertEquals(50L, dto.getPatientId());
        assertEquals(25L, dto.getCaregiverId());
        assertEquals("Living Room", dto.getLastSeenLocation());
        assertEquals(lastSeen, dto.getLastSeenDate());
        assertEquals(ItemStatus.LOST, dto.getStatus());
        assertEquals(ItemPriority.MEDIUM, dto.getPriority());
        assertEquals("http://setter.url", dto.getImageUrl());
        assertEquals(created, dto.getCreatedAt());
    }

    @Test
    void testEqualsAndHashCode() {
        LostItemDTO dto1 = LostItemDTO.builder().id(1L).title("Item").build();
        LostItemDTO dto2 = LostItemDTO.builder().id(1L).title("Item").build();
        LostItemDTO dto3 = LostItemDTO.builder().id(2L).title("Different").build();

        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
        assertNotEquals(dto1, dto3);
        assertNotEquals(dto1.hashCode(), dto3.hashCode());
    }

    @Test
    void testToString() {
        LostItemDTO dto = LostItemDTO.builder().id(1L).title("Test").build();
        String str = dto.toString();

        assertNotNull(str);
        assertTrue(str.contains("LostItemDTO"));
        assertTrue(str.contains("1"));
        assertTrue(str.contains("Test"));
    }

    @Test
    void testPartialBuilding() {
        LostItemDTO dto = LostItemDTO.builder()
                .id(3L)
                .title("Partial")
                .category(ItemCategory.ACCESSORY)
                .build();

        assertEquals(3L, dto.getId());
        assertEquals("Partial", dto.getTitle());
        assertEquals(ItemCategory.ACCESSORY, dto.getCategory());
        assertNull(dto.getDescription());
        assertNull(dto.getPatientId());
        assertNull(dto.getStatus());
    }
}
