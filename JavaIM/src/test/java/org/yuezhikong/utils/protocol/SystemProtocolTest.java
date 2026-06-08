package org.yuezhikong.utils.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemProtocolTest {

    @Test
    void setAndGet_type() {
        SystemProtocol protocol = new SystemProtocol();
        protocol.setType("Chat");
        assertEquals("Chat", protocol.getType());
    }

    @Test
    void setAndGet_message() {
        SystemProtocol protocol = new SystemProtocol();
        protocol.setMessage("Welcome to JavaIM");
        assertEquals("Welcome to JavaIM", protocol.getMessage());
    }

    @Test
    void setType_totp() {
        SystemProtocol protocol = new SystemProtocol();
        protocol.setType("TOTP");
        assertEquals("TOTP", protocol.getType());
    }

    @Test
    void setType_error() {
        SystemProtocol protocol = new SystemProtocol();
        protocol.setType("Error");
        protocol.setMessage("Invalid password");

        assertEquals("Error", protocol.getType());
        assertEquals("Invalid password", protocol.getMessage());
    }

    @Test
    void newInstance_allFieldsNull() {
        SystemProtocol protocol = new SystemProtocol();
        assertNull(protocol.getType());
        assertNull(protocol.getMessage());
    }

    @Test
    void equals_sameValues_areEqual() {
        SystemProtocol p1 = new SystemProtocol();
        p1.setType("DisplayMessage");
        p1.setMessage("Hello");

        SystemProtocol p2 = new SystemProtocol();
        p2.setType("DisplayMessage");
        p2.setMessage("Hello");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
