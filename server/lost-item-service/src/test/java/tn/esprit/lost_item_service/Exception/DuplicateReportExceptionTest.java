package tn.esprit.lost_item_service.Exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateReportExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Duplicate report already exists";
        DuplicateReportException exception = new DuplicateReportException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testIsRuntimeException() {
        DuplicateReportException exception = new DuplicateReportException("test");
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(DuplicateReportException.class, () -> {
            throw new DuplicateReportException("Report for this item already exists");
        });
    }

    @Test
    void testExceptionMessage() {
        String expectedMessage = "A search report already exists for item 5 from user 10";
        DuplicateReportException exception = new DuplicateReportException(expectedMessage);
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testExceptionStackTrace() {
        DuplicateReportException exception = new DuplicateReportException("test");
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }

    @Test
    void testMultipleInstances() {
        DuplicateReportException ex1 = new DuplicateReportException("msg1");
        DuplicateReportException ex2 = new DuplicateReportException("msg2");
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
    }
}
