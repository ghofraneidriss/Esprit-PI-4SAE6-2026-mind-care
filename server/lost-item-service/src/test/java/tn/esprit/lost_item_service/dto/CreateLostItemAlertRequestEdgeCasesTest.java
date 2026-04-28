package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.Entity.AlertLevel;

import static org.junit.jupiter.api.Assertions.*;

class CreateLostItemAlertRequestEdgeCasesTest {

    @Test
    void testEqualsWithSameObject() {
        CreateLostItemAlertRequest req = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .build();
        assertEquals(req, req);
    }

    @Test
    void testEqualsWithNull() {
        CreateLostItemAlertRequest req = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .build();
        assertFalse(req.equals(null));
    }

    @Test
    void testEqualsWithDifferentType() {
        CreateLostItemAlertRequest req = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .build();
        assertFalse(req.equals("not a request"));
    }

    @Test
    void testEqualsWithDifferentLostItemId() {
        CreateLostItemAlertRequest req1 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .build();
        CreateLostItemAlertRequest req2 = CreateLostItemAlertRequest.builder()
                .lostItemId(2L)
                .patientId(5L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .build();
        assertNotEquals(req1, req2);
    }

    @Test
    void testEqualsWithDifferentPatientId() {
        CreateLostItemAlertRequest req1 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .build();
        CreateLostItemAlertRequest req2 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(10L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .build();
        assertNotEquals(req1, req2);
    }

    @Test
    void testEqualsWithDifferentTitle() {
        CreateLostItemAlertRequest req1 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert1")
                .level(AlertLevel.HIGH)
                .build();
        CreateLostItemAlertRequest req2 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert2")
                .level(AlertLevel.HIGH)
                .build();
        assertNotEquals(req1, req2);
    }

    @Test
    void testEqualsWithDifferentLevel() {
        CreateLostItemAlertRequest req1 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .build();
        CreateLostItemAlertRequest req2 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .level(AlertLevel.LOW)
                .build();
        assertNotEquals(req1, req2);
    }

    @Test
    void testHashCodeConsistency() {
        CreateLostItemAlertRequest req = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .level(AlertLevel.CRITICAL)
                .build();
        int hash1 = req.hashCode();
        int hash2 = req.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashCodeWithEqualObjects() {
        CreateLostItemAlertRequest req1 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .build();
        CreateLostItemAlertRequest req2 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .build();
        assertEquals(req1.hashCode(), req2.hashCode());
    }

    @Test
    void testToStringContainsFields() {
        CreateLostItemAlertRequest req = CreateLostItemAlertRequest.builder()
                .lostItemId(10L)
                .patientId(5L)
                .title("MyAlert")
                .level(AlertLevel.CRITICAL)
                .build();

        String str = req.toString();
        assertTrue(str.contains("10"));
        assertTrue(str.contains("5"));
        assertTrue(str.contains("MyAlert"));
        assertTrue(str.contains("CRITICAL"));
    }

    @Test
    void testAllAlertLevelValues() {
        for (AlertLevel level : AlertLevel.values()) {
            CreateLostItemAlertRequest req = CreateLostItemAlertRequest.builder()
                    .lostItemId(1L)
                    .patientId(5L)
                    .title("Alert")
                    .level(level)
                    .build();
            assertEquals(level, req.getLevel());
        }
    }

    @Test
    void testNullFieldsEquality() {
        CreateLostItemAlertRequest req1 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .description(null)
                .caregiverId(null)
                .level(AlertLevel.HIGH)
                .build();

        CreateLostItemAlertRequest req2 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .title("Alert")
                .description(null)
                .caregiverId(null)
                .level(AlertLevel.HIGH)
                .build();

        assertEquals(req1, req2);
    }

    @Test
    void testWithCaregiverId() {
        CreateLostItemAlertRequest req1 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .caregiverId(3L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .build();

        CreateLostItemAlertRequest req2 = CreateLostItemAlertRequest.builder()
                .lostItemId(1L)
                .patientId(5L)
                .caregiverId(4L)
                .title("Alert")
                .level(AlertLevel.HIGH)
                .build();

        assertNotEquals(req1, req2);
    }
}
