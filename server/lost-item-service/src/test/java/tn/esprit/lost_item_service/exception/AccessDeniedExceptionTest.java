package tn.esprit.lost_item_service.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccessDeniedExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Access denied to this resource";
        AccessDeniedException exception = new AccessDeniedException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void testIsRuntimeException() {
        AccessDeniedException exception = new AccessDeniedException("test");
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testThrowAndCatch() {
        assertThrows(AccessDeniedException.class, () -> {
            throw new AccessDeniedException("User does not have permission");
        });
    }

    @Test
    void testExceptionMessage() {
        String expectedMessage = "User id 5 does not have access to lost item 10";
        AccessDeniedException exception = new AccessDeniedException(expectedMessage);
        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("does not have access"));
    }

    @Test
    void testExceptionStackTrace() {
        AccessDeniedException exception = new AccessDeniedException("test");
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }
}
