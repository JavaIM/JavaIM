package org.yuezhikong.utils.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginProtocolTest {

    @Test
    void setAndGet_loginPacketHead() {
        LoginProtocol protocol = new LoginProtocol();
        LoginProtocol.LoginPacketHeadBean head = new LoginProtocol.LoginPacketHeadBean();
        head.setType("passwd");
        protocol.setLoginPacketHead(head);

        assertEquals("passwd", protocol.getLoginPacketHead().getType());
    }

    @Test
    void setAndGet_normalLogin() {
        LoginProtocol protocol = new LoginProtocol();
        LoginProtocol.LoginPacketBodyBean body = new LoginProtocol.LoginPacketBodyBean();
        LoginProtocol.LoginPacketBodyBean.NormalLoginBean normalLogin =
                new LoginProtocol.LoginPacketBodyBean.NormalLoginBean();
        normalLogin.setUserName("testuser");
        normalLogin.setPasswd("password123");

        body.setNormalLogin(normalLogin);
        protocol.setLoginPacketBody(body);

        assertEquals("testuser", protocol.getLoginPacketBody().getNormalLogin().getUserName());
        assertEquals("password123", protocol.getLoginPacketBody().getNormalLogin().getPasswd());
    }

    @Test
    void setAndGet_reLogin() {
        LoginProtocol protocol = new LoginProtocol();
        LoginProtocol.LoginPacketBodyBean body = new LoginProtocol.LoginPacketBodyBean();
        LoginProtocol.LoginPacketBodyBean.ReLoginBean reLogin =
                new LoginProtocol.LoginPacketBodyBean.ReLoginBean();
        reLogin.setToken("abc123token");

        body.setReLogin(reLogin);
        protocol.setLoginPacketBody(body);

        assertEquals("abc123token", protocol.getLoginPacketBody().getReLogin().getToken());
    }

    @Test
    void newInstance_allFieldsNull() {
        LoginProtocol protocol = new LoginProtocol();
        assertNull(protocol.getLoginPacketHead());
        assertNull(protocol.getLoginPacketBody());
    }

    @Test
    void headBean_equals() {
        LoginProtocol.LoginPacketHeadBean h1 = new LoginProtocol.LoginPacketHeadBean();
        h1.setType("token");

        LoginProtocol.LoginPacketHeadBean h2 = new LoginProtocol.LoginPacketHeadBean();
        h2.setType("token");

        assertEquals(h1, h2);
        assertEquals(h1.hashCode(), h2.hashCode());
    }
}
