package org.yuezhikong.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChecksTest {

    @Test
    void checkArgument_expressionTrue_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                checks.checkArgument(true, "error message"));
    }

    @Test
    void checkArgument_expressionFalse_doesNotThrow() {
        assertDoesNotThrow(() -> checks.checkArgument(false, "should not throw"));
    }

    @Test
    void checkArgument_throwsWithCorrectMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                checks.checkArgument(true, "test error"));
        assertEquals("test error", ex.getMessage());
    }

    @Test
    void checkState_expressionTrue_throwsIllegalStateException() {
        assertThrows(IllegalStateException.class, () ->
                checks.checkState(true, "error message"));
    }

    @Test
    void checkState_expressionFalse_doesNotThrow() {
        assertDoesNotThrow(() -> checks.checkState(false, "should not throw"));
    }

    @Test
    void checkState_throwsWithCorrectMessage() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                checks.checkState(true, "state error"));
        assertEquals("state error", ex.getMessage());
    }
}
