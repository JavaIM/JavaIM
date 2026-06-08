package org.yuezhikong.utils.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatProtocolTest {

    @Test
    void setAndGet_message() {
        ChatProtocol protocol = new ChatProtocol();
        protocol.setMessage("Hello, World!");
        assertEquals("Hello, World!", protocol.getMessage());
    }

    @Test
    void setAndGet_sourceUserName() {
        ChatProtocol protocol = new ChatProtocol();
        protocol.setSourceUserName("Alice");
        assertEquals("Alice", protocol.getSourceUserName());
    }

    @Test
    void newInstance_allFieldsNull() {
        ChatProtocol protocol = new ChatProtocol();
        assertNull(protocol.getMessage());
        assertNull(protocol.getSourceUserName());
    }

    @Test
    void equals_sameValues_areEqual() {
        ChatProtocol p1 = new ChatProtocol();
        p1.setMessage("Hi");
        p1.setSourceUserName("Bob");

        ChatProtocol p2 = new ChatProtocol();
        p2.setMessage("Hi");
        p2.setSourceUserName("Bob");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
