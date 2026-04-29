package tn.esprit.lost_item_service.dto;

import org.junit.jupiter.api.Test;
import tn.esprit.lost_item_service.entity.ItemCategory;
import tn.esprit.lost_item_service.entity.ItemPriority;
import tn.esprit.lost_item_service.entity.ItemStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LostItemDTOEdgeCasesTest {

    @Test
    void testEqualsWithSameObject() {
        LostItemDTO dto = LostItemDTO.builder().id(1L).title("Test").build();
        assertEquals(dto, dto);
    }

    @Test
    void testEqualsWithNull() {
        LostItemDTO dto = LostItemDTO.builder().id(1L).title("Test").build();
        assertNotEquals(null, dto);
    }

    @Test
    void testEqualsWithDifferentType() {
        LostItemDTO dto = LostItemDTO.builder().id(1L).title("Test").build();
        assertNotEquals("not a dto", dto);
    }

    @Test
    void testEqualsWithDifferentId() {
        LostItemDTO dto1 = LostItemDTO.builder().id(1L).title("Test").build();
        LostItemDTO dto2 = LostItemDTO.builder().id(2L).title("Test").build();
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithDifferentTitle() {
        LostItemDTO dto1 = LostItemDTO.builder().id(1L).title("Test1").build();
        LostItemDTO dto2 = LostItemDTO.builder().id(1L).title("Test2").build();
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithAllFieldsDifferent() {
        LocalDate date1 = LocalDate.now();
        LocalDate date2 = LocalDate.now().minusDays(1);

        LostItemDTO dto1 = LostItemDTO.builder()
                .id(1L)
                .title("Item1")
                .description("Desc1")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .caregiverId(2L)
                .lastSeenLocation("Location1")
                .lastSeenDate(date1)
                .status(ItemStatus.LOST)
                .priority(ItemPriority.HIGH)
                .imageUrl("url1")
                .build();

        LostItemDTO dto2 = LostItemDTO.builder()
                .id(2L)
                .title("Item2")
                .description("Desc2")
                .category(ItemCategory.DOCUMENT)
                .patientId(2L)
                .caregiverId(3L)
                .lastSeenLocation("Location2")
                .lastSeenDate(date2)
                .status(ItemStatus.FOUND)
                .priority(ItemPriority.LOW)
                .imageUrl("url2")
                .build();

        assertNotEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithNullFields() {
        LostItemDTO dto1 = LostItemDTO.builder().id(1L).title(null).build();
        LostItemDTO dto2 = LostItemDTO.builder().id(1L).title(null).build();
        assertEquals(dto1, dto2);
    }

    @Test
    void testEqualsWithMixedNullFields() {
        LostItemDTO dto1 = LostItemDTO.builder().id(1L).title(null).category(ItemCategory.MEDICATION).build();
        LostItemDTO dto2 = LostItemDTO.builder().id(1L).title("Test").category(ItemCategory.MEDICATION).build();
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testHashCodeConsistency() {
        LostItemDTO dto = LostItemDTO.builder().id(1L).title("Test").category(ItemCategory.CLOTHING).build();
        int hash1 = dto.hashCode();
        int hash2 = dto.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashCodeWithEqualObjects() {
        LostItemDTO dto1 = LostItemDTO.builder().id(1L).title("Test").build();
        LostItemDTO dto2 = LostItemDTO.builder().id(1L).title("Test").build();
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testHashCodeWithDifferentObjects() {
        LostItemDTO dto1 = LostItemDTO.builder().id(1L).title("Test1").build();
        LostItemDTO dto2 = LostItemDTO.builder().id(1L).title("Test2").build();
        assertNotEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testToStringContainsFields() {
        LostItemDTO dto = LostItemDTO.builder()
                .id(5L)
                .title("MyItem")
                .category(ItemCategory.MEDICATION)
                .patientId(10L)
                .build();

        String str = dto.toString();
        assertTrue(str.contains("5"));
        assertTrue(str.contains("MyItem"));
        assertTrue(str.contains("MEDICATION"));
    }

    @Test
    void testToStringWithNullFields() {
        LostItemDTO dto = LostItemDTO.builder().id(1L).title(null).build();
        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("LostItemDTO"));
    }

    @Test
    void testBuilderWithAllNull() {
        LostItemDTO dto = LostItemDTO.builder().build();
        assertNull(dto.getId());
        assertNull(dto.getTitle());
        assertNull(dto.getCategory());
    }

    @Test
    void testSettersChaining() {
        LostItemDTO dto = new LostItemDTO();
        dto.setId(1L);
        dto.setTitle("Title");
        dto.setCategory(ItemCategory.DOCUMENT);

        assertEquals(1L, dto.getId());
        assertEquals("Title", dto.getTitle());
        assertEquals(ItemCategory.DOCUMENT, dto.getCategory());
    }

    @Test
    void testWithAllCategoryValues() {
        for (ItemCategory cat : ItemCategory.values()) {
            LostItemDTO dto = LostItemDTO.builder().category(cat).build();
            assertEquals(cat, dto.getCategory());
            assertNotNull(dto.toString());
        }
    }

    @Test
    void testWithAllStatusValues() {
        for (ItemStatus status : ItemStatus.values()) {
            LostItemDTO dto = LostItemDTO.builder().status(status).build();
            assertEquals(status, dto.getStatus());
        }
    }

    @Test
    void testWithAllPriorityValues() {
        for (ItemPriority priority : ItemPriority.values()) {
            LostItemDTO dto = LostItemDTO.builder().priority(priority).build();
            assertEquals(priority, dto.getPriority());
        }
    }
}
