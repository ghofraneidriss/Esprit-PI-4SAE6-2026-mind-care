package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.Entity.ItemCategory;
import tn.esprit.lost_item_service.Entity.ItemPriority;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CreateLostItemRequestEdgeCasesTest {

    @Test
    void testEqualsWithSameObject() {
        CreateLostItemRequest req = CreateLostItemRequest.builder().title("Test").category(ItemCategory.MEDICATION).patientId(1L).build();
        assertEquals(req, req);
    }

    @Test
    void testEqualsWithNull() {
        CreateLostItemRequest req = CreateLostItemRequest.builder().title("Test").category(ItemCategory.MEDICATION).patientId(1L).build();
        assertFalse(req.equals(null));
    }

    @Test
    void testEqualsWithDifferentType() {
        CreateLostItemRequest req = CreateLostItemRequest.builder().title("Test").category(ItemCategory.MEDICATION).patientId(1L).build();
        assertFalse(req.equals("not a request"));
    }

    @Test
    void testEqualsWithDifferentTitle() {
        CreateLostItemRequest req1 = CreateLostItemRequest.builder().title("Title1").category(ItemCategory.MEDICATION).patientId(1L).build();
        CreateLostItemRequest req2 = CreateLostItemRequest.builder().title("Title2").category(ItemCategory.MEDICATION).patientId(1L).build();
        assertNotEquals(req1, req2);
    }

    @Test
    void testEqualsWithDifferentCategory() {
        CreateLostItemRequest req1 = CreateLostItemRequest.builder().title("Test").category(ItemCategory.MEDICATION).patientId(1L).build();
        CreateLostItemRequest req2 = CreateLostItemRequest.builder().title("Test").category(ItemCategory.DOCUMENT).patientId(1L).build();
        assertNotEquals(req1, req2);
    }

    @Test
    void testEqualsWithDifferentPatientId() {
        CreateLostItemRequest req1 = CreateLostItemRequest.builder().title("Test").category(ItemCategory.MEDICATION).patientId(1L).build();
        CreateLostItemRequest req2 = CreateLostItemRequest.builder().title("Test").category(ItemCategory.MEDICATION).patientId(2L).build();
        assertNotEquals(req1, req2);
    }

    @Test
    void testEqualsWithDifferentPriority() {
        CreateLostItemRequest req1 = CreateLostItemRequest.builder().title("Test").category(ItemCategory.MEDICATION).patientId(1L).priority(ItemPriority.HIGH).build();
        CreateLostItemRequest req2 = CreateLostItemRequest.builder().title("Test").category(ItemCategory.MEDICATION).patientId(1L).priority(ItemPriority.LOW).build();
        assertNotEquals(req1, req2);
    }

    @Test
    void testHashCodeConsistency() {
        CreateLostItemRequest req = CreateLostItemRequest.builder()
                .title("Test")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .priority(ItemPriority.CRITICAL)
                .build();
        int hash1 = req.hashCode();
        int hash2 = req.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashCodeWithEqualObjects() {
        CreateLostItemRequest req1 = CreateLostItemRequest.builder().title("Test").category(ItemCategory.MEDICATION).patientId(1L).build();
        CreateLostItemRequest req2 = CreateLostItemRequest.builder().title("Test").category(ItemCategory.MEDICATION).patientId(1L).build();
        assertEquals(req1.hashCode(), req2.hashCode());
    }

    @Test
    void testToStringContainsFields() {
        CreateLostItemRequest req = CreateLostItemRequest.builder()
                .title("MyItem")
                .category(ItemCategory.DOCUMENT)
                .patientId(5L)
                .build();

        String str = req.toString();
        assertTrue(str.contains("MyItem"));
        assertTrue(str.contains("DOCUMENT"));
        assertTrue(str.contains("5"));
    }

    @Test
    void testAllCategoryValues() {
        for (ItemCategory cat : ItemCategory.values()) {
            CreateLostItemRequest req = CreateLostItemRequest.builder()
                    .title("Test")
                    .category(cat)
                    .patientId(1L)
                    .build();
            assertEquals(cat, req.getCategory());
        }
    }

    @Test
    void testAllPriorityValues() {
        for (ItemPriority priority : ItemPriority.values()) {
            CreateLostItemRequest req = CreateLostItemRequest.builder()
                    .title("Test")
                    .category(ItemCategory.MEDICATION)
                    .patientId(1L)
                    .priority(priority)
                    .build();
            assertEquals(priority, req.getPriority());
        }
    }

    @Test
    void testWithDates() {
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.now().minusDays(3);

        CreateLostItemRequest req1 = CreateLostItemRequest.builder()
                .title("Test")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .lastSeenDate(date1)
                .build();

        CreateLostItemRequest req2 = CreateLostItemRequest.builder()
                .title("Test")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .lastSeenDate(date2)
                .build();

        assertNotEquals(req1, req2);
    }

    @Test
    void testNullFieldsEquality() {
        CreateLostItemRequest req1 = CreateLostItemRequest.builder()
                .title("Test")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .description(null)
                .caregiverId(null)
                .priority(null)
                .build();

        CreateLostItemRequest req2 = CreateLostItemRequest.builder()
                .title("Test")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .description(null)
                .caregiverId(null)
                .priority(null)
                .build();

        assertEquals(req1, req2);
    }
}
