package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.Entity.AlertLevel;

import static org.junit.jupiter.api.Assertions.*;

class CreateLostItemAlertRequestTest {

    @Test
    void testDefaultConstructor() {
        CreateLostItemAlertRequest request = new CreateLostItemAlertRequest();
        assertNull(request.getTitle());
        assertNull(request.getLevel());
    }

    @Test
    void testAllArgsConstructor() {
        CreateLostItemAlertRequest request = new CreateLostItemAlertRequest(
                10L, 5L, 3L, "Alert Title", "Alert Description", AlertLevel.CRITICAL
        );

        assertEquals(10L, request.getLostItemId());
        assertEquals(5L, request.getPatientId());
        assertEquals(3L, request.getCaregiverId());
        assertEquals("Alert Title", request.getTitle());
        assertEquals("Alert Description", request.getDescription());
        assertEquals(AlertLevel.CRITICAL, request.getLevel());
    }

    @Test
    void testBuilderPattern() {
        CreateLostItemAlertRequest request = CreateLostItemAlertRequest.builder()
                .lostItemId(20L)
                .patientId(15L)
                .caregiverId(8L)
                .title("Built Alert")
                .description("Built Description")
                .level(AlertLevel.HIGH)
                .build();

        assertEquals(20L, request.getLostItemId());
        assertEquals(15L, request.getPatientId());
        assertEquals(8L, request.getCaregiverId());
        assertEquals("Built Alert", request.getTitle());
        assertEquals("Built Description", request.getDescription());
        assertEquals(AlertLevel.HIGH, request.getLevel());
    }

    @Test
    void testSettersAndGetters() {
        CreateLostItemAlertRequest request = new CreateLostItemAlertRequest();

        request.setLostItemId(50L);
        request.setPatientId(25L);
        request.setCaregiverId(10L);
        request.setTitle("Setter Alert");
        request.setDescription("Setter Description");
        request.setLevel(AlertLevel.MEDIUM);

        assertEquals(50L, request.getLostItemId());
        assertEquals(25L, request.getPatientId());
        assertEquals(10L, request.getCaregiverId());
        assertEquals("Setter Alert", request.getTitle());
        assertEquals("Setter Description", request.getDescription());
        assertEquals(AlertLevel.MEDIUM, request.getLevel());
    }

    @Test
    void testEqualsAndHashCode() {
        CreateLostItemAlertRequest request1 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert1")
                .level(AlertLevel.HIGH)
                .build();

        CreateLostItemAlertRequest request2 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert1")
                .level(AlertLevel.HIGH)
                .build();

        CreateLostItemAlertRequest request3 = CreateLostItemAlertRequest.builder()
                .lostItemId(2L)
                .patientId(10L)
                .title("Alert2")
                .level(AlertLevel.LOW)
                .build();

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotEquals(request1, request3);
        assertNotEquals(request1.hashCode(), request3.hashCode());
    }

    @Test
    void testToString() {
        CreateLostItemAlertRequest request = CreateLostItemAlertRequest.builder()
                .title("Test Alert")
                .level(AlertLevel.CRITICAL)
                .build();

        String str = request.toString();
        assertNotNull(str);
        assertTrue(str.contains("CreateLostItemAlertRequest"));
        assertTrue(str.contains("Test Alert"));
        assertTrue(str.contains("CRITICAL"));
    }

    @Test
    void testAllAlertLevels() {
        for (AlertLevel level : AlertLevel.values()) {
            CreateLostItemAlertRequest request = CreateLostItemAlertRequest.builder()
                    .level(level)
                    .build();
            assertEquals(level, request.getLevel());
        }
    }

    @Test
    void testMinimalRequest() {
        CreateLostItemAlertRequest request = CreateLostItemAlertRequest.builder()
                .lostItemId(10L)
                .patientId(5L)
                .title("Minimal Alert")
                .level(AlertLevel.MEDIUM)
                .build();

        assertEquals(10L, request.getLostItemId());
        assertEquals(5L, request.getPatientId());
        assertEquals("Minimal Alert", request.getTitle());
        assertEquals(AlertLevel.MEDIUM, request.getLevel());
        assertNull(request.getCaregiverId());
        assertNull(request.getDescription());
    }

    @Test
    void testWithNullOptionalFields() {
        CreateLostItemAlertRequest request = CreateLostItemAlertRequest.builder()
                .lostItemId(10L)
                .patientId(5L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .caregiverId(null)
                .description(null)
                .build();

        assertNull(request.getCaregiverId());
        assertNull(request.getDescription());
        assertNotNull(request.getTitle());
        assertEquals(AlertLevel.HIGH, request.getLevel());
    }

    @Test
    void testWithLongStrings() {
        String longTitle = "A".repeat(100);
        String longDescription = "B".repeat(500);

        CreateLostItemAlertRequest request = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(1L)
                .title(longTitle)
                .description(longDescription)
                .level(AlertLevel.CRITICAL)
                .build();

        assertEquals(longTitle, request.getTitle());
        assertEquals(longDescription, request.getDescription());
    }
}
