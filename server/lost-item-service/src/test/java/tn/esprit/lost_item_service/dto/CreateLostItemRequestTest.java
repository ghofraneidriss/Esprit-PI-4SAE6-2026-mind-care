package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.Entity.ItemCategory;
import tn.esprit.lost_item_service.Entity.ItemPriority;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CreateLostItemRequestTest {

    @Test
    void testDefaultConstructor() {
        CreateLostItemRequest request = new CreateLostItemRequest();
        assertNull(request.getTitle());
        assertNull(request.getCategory());
        assertNull(request.getPatientId());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate lastSeen = LocalDate.now();

        CreateLostItemRequest request = new CreateLostItemRequest(
                "Test Item", "Test Description", ItemCategory.MEDICATION,
                10L, 5L, "Kitchen", lastSeen, ItemPriority.CRITICAL, "http://image.url"
        );

        assertEquals("Test Item", request.getTitle());
        assertEquals("Test Description", request.getDescription());
        assertEquals(ItemCategory.MEDICATION, request.getCategory());
        assertEquals(10L, request.getPatientId());
        assertEquals(5L, request.getCaregiverId());
        assertEquals("Kitchen", request.getLastSeenLocation());
        assertEquals(lastSeen, request.getLastSeenDate());
        assertEquals(ItemPriority.CRITICAL, request.getPriority());
        assertEquals("http://image.url", request.getImageUrl());
    }

    @Test
    void testBuilderPattern() {
        LocalDate lastSeen = LocalDate.now();

        CreateLostItemRequest request = CreateLostItemRequest.builder()
                .title("Built Item")
                .description("Built Description")
                .category(ItemCategory.CLOTHING)
                .patientId(20L)
                .caregiverId(15L)
                .lastSeenLocation("Bedroom")
                .lastSeenDate(lastSeen)
                .priority(ItemPriority.MEDIUM)
                .imageUrl("http://built.url")
                .build();

        assertEquals("Built Item", request.getTitle());
        assertEquals("Built Description", request.getDescription());
        assertEquals(ItemCategory.CLOTHING, request.getCategory());
        assertEquals(20L, request.getPatientId());
        assertEquals(15L, request.getCaregiverId());
        assertEquals("Bedroom", request.getLastSeenLocation());
        assertEquals(lastSeen, request.getLastSeenDate());
        assertEquals(ItemPriority.MEDIUM, request.getPriority());
        assertEquals("http://built.url", request.getImageUrl());
    }

    @Test
    void testSettersAndGetters() {
        CreateLostItemRequest request = new CreateLostItemRequest();

        request.setTitle("Setter Item");
        request.setDescription("Setter Description");
        request.setCategory(ItemCategory.DOCUMENT);
        request.setPatientId(30L);
        request.setCaregiverId(12L);
        request.setLastSeenLocation("Office");
        LocalDate lastSeen = LocalDate.now();
        request.setLastSeenDate(lastSeen);
        request.setPriority(ItemPriority.LOW);
        request.setImageUrl("http://setter.url");

        assertEquals("Setter Item", request.getTitle());
        assertEquals("Setter Description", request.getDescription());
        assertEquals(ItemCategory.DOCUMENT, request.getCategory());
        assertEquals(30L, request.getPatientId());
        assertEquals(12L, request.getCaregiverId());
        assertEquals("Office", request.getLastSeenLocation());
        assertEquals(lastSeen, request.getLastSeenDate());
        assertEquals(ItemPriority.LOW, request.getPriority());
        assertEquals("http://setter.url", request.getImageUrl());
    }

    @Test
    void testEqualsAndHashCode() {
        CreateLostItemRequest request1 = CreateLostItemRequest.builder()
                .title("Item1")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .build();

        CreateLostItemRequest request2 = CreateLostItemRequest.builder()
                .title("Item1")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .build();

        CreateLostItemRequest request3 = CreateLostItemRequest.builder()
                .title("Item2")
                .category(ItemCategory.CLOTHING)
                .patientId(2L)
                .build();

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1, request3);
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    void testToString() {
        CreateLostItemRequest request = CreateLostItemRequest.builder()
                .title("Test")
                .category(ItemCategory.ACCESSORY)
                .build();

        String str = request.toString();
        assertNotNull(str);
        assertTrue(str.contains("CreateLostItemRequest"));
        assertTrue(str.contains("Test"));
        assertTrue(str.contains("ACCESSORY"));
    }

    @Test
    void testAllItemCategories() {
        for (ItemCategory category : ItemCategory.values()) {
            CreateLostItemRequest request = CreateLostItemRequest.builder()
                    .title("Test")
                    .category(category)
                    .patientId(1L)
                    .build();
            assertEquals(category, request.getCategory());
        }
    }

    @Test
    void testAllItemPriorities() {
        for (ItemPriority priority : ItemPriority.values()) {
            CreateLostItemRequest request = CreateLostItemRequest.builder()
                    .priority(priority)
                    .build();
            assertEquals(priority, request.getPriority());
        }
    }

    @Test
    void testMinimalRequest() {
        CreateLostItemRequest request = CreateLostItemRequest.builder()
                .title("Minimal")
                .category(ItemCategory.MEDICATION)
                .patientId(5L)
                .build();

        assertEquals("Minimal", request.getTitle());
        assertEquals(ItemCategory.MEDICATION, request.getCategory());
        assertEquals(5L, request.getPatientId());
        assertNull(request.getDescription());
        assertNull(request.getCaregiverId());
        assertNull(request.getLastSeenLocation());
        assertNull(request.getLastSeenDate());
        assertNull(request.getPriority());
        assertNull(request.getImageUrl());
    }

    @Test
    void testWithNullOptionalFields() {
        CreateLostItemRequest request = CreateLostItemRequest.builder()
                .title("Item")
                .category(ItemCategory.CLOTHING)
                .patientId(1L)
                .caregiverId(null)
                .lastSeenLocation(null)
                .priority(null)
                .build();

        assertNull(request.getCaregiverId());
        assertNull(request.getLastSeenLocation());
        assertNull(request.getPriority());
        assertNotNull(request.getTitle());
    }
}
