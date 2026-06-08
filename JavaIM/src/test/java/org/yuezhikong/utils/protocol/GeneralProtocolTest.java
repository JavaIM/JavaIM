package org.yuezhikong.utils.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneralProtocolTest {

    @Test
    void setAndGet_protocolVersion() {
        GeneralProtocol protocol = new GeneralProtocol();
        protocol.setProtocolVersion(13);
        assertEquals(13, protocol.getProtocolVersion());
    }

    @Test
    void setAndGet_protocolName() {
        GeneralProtocol protocol = new GeneralProtocol();
        protocol.setProtocolName("ChatProtocol");
        assertEquals("ChatProtocol", protocol.getProtocolName());
    }

    @Test
    void setAndGet_protocolData() {
        GeneralProtocol protocol = new GeneralProtocol();
        protocol.setProtocolData("{\"message\":\"hello\"}");
        assertEquals("{\"message\":\"hello\"}", protocol.getProtocolData());
    }

    @Test
    void equals_sameValues_areEqual() {
        GeneralProtocol p1 = new GeneralProtocol();
        p1.setProtocolVersion(13);
        p1.setProtocolName("ChatProtocol");
        p1.setProtocolData("data");

        GeneralProtocol p2 = new GeneralProtocol();
        p2.setProtocolVersion(13);
        p2.setProtocolName("ChatProtocol");
        p2.setProtocolData("data");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void equals_differentValues_areNotEqual() {
        GeneralProtocol p1 = new GeneralProtocol();
        p1.setProtocolVersion(13);
        p1.setProtocolName("ChatProtocol");

        GeneralProtocol p2 = new GeneralProtocol();
        p2.setProtocolVersion(14);
        p2.setProtocolName("LoginProtocol");

        assertNotEquals(p1, p2);
    }

    @Test
    void toString_containsFieldValues() {
        GeneralProtocol protocol = new GeneralProtocol();
        protocol.setProtocolVersion(13);
        protocol.setProtocolName("SystemProtocol");

        String str = protocol.toString();
        assertTrue(str.contains("13"));
        assertTrue(str.contains("SystemProtocol"));
    }
}
