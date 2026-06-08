package org.yuezhikong.utils.protocol;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransferProtocolTest {

    @Test
    void setAndGet_transferHead() {
        TransferProtocol protocol = new TransferProtocol();
        TransferProtocol.TransferProtocolHeadBean head =
                new TransferProtocol.TransferProtocolHeadBean();
        head.setTargetUserName("Bob");
        head.setType("transfer");
        protocol.setTransferProtocolHead(head);

        assertEquals("Bob", protocol.getTransferProtocolHead().getTargetUserName());
        assertEquals("transfer", protocol.getTransferProtocolHead().getType());
    }

    @Test
    void setAndGet_transferBody() {
        TransferProtocol protocol = new TransferProtocol();
        List<TransferProtocol.TransferProtocolBodyBean> bodyList = new ArrayList<>();

        TransferProtocol.TransferProtocolBodyBean body1 =
                new TransferProtocol.TransferProtocolBodyBean();
        body1.setData("file1.txt");
        bodyList.add(body1);

        TransferProtocol.TransferProtocolBodyBean body2 =
                new TransferProtocol.TransferProtocolBodyBean();
        body2.setData("content");
        bodyList.add(body2);

        protocol.setTransferProtocolBody(bodyList);

        assertEquals(2, protocol.getTransferProtocolBody().size());
        assertEquals("file1.txt", protocol.getTransferProtocolBody().get(0).getData());
        assertEquals("content", protocol.getTransferProtocolBody().get(1).getData());
    }

    @Test
    void newInstance_allFieldsNull() {
        TransferProtocol protocol = new TransferProtocol();
        assertNull(protocol.getTransferProtocolHead());
        assertNull(protocol.getTransferProtocolBody());
    }

    @Test
    void headBean_typeFileList() {
        TransferProtocol.TransferProtocolHeadBean head =
                new TransferProtocol.TransferProtocolHeadBean();
        head.setType("fileList");

        assertEquals("fileList", head.getType());
    }

    @Test
    void headBean_equals() {
        TransferProtocol.TransferProtocolHeadBean h1 =
                new TransferProtocol.TransferProtocolHeadBean();
        h1.setTargetUserName("Alice");
        h1.setType("upload");

        TransferProtocol.TransferProtocolHeadBean h2 =
                new TransferProtocol.TransferProtocolHeadBean();
        h2.setTargetUserName("Alice");
        h2.setType("upload");

        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
    }
}
