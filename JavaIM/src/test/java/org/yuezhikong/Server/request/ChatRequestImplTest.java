package org.yuezhikong.Server.request;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.yuezhikong.Server.IServer;
import org.yuezhikong.Server.ServerTools;
import org.yuezhikong.Server.api.api;
import org.yuezhikong.Server.plugin.PluginManager;
import org.yuezhikong.Server.plugin.event.events.User.UserChatEvent;
import org.yuezhikong.Server.plugin.event.events.User.UserCommandEvent;
import org.yuezhikong.Server.userData.Permission;
import org.yuezhikong.Server.userData.user;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatRequestImplTest {

    private MockedStatic<ServerTools> mockedServerTools;
    private IServer mockServer;
    private api mockApi;
    private PluginManager mockPluginManager;
    private ChatRequestImpl chatRequest;

    @BeforeEach
    void setUp() {
        mockedServerTools = mockStatic(ServerTools.class);

        mockServer = mock(IServer.class);
        mockApi = mock(api.class);
        mockPluginManager = mock(PluginManager.class);

        when(mockServer.getServerAPI()).thenReturn(mockApi);
        when(mockServer.getPluginManager()).thenReturn(mockPluginManager);
        mockedServerTools.when(ServerTools::getServerInstanceOrThrow).thenReturn(mockServer);

        chatRequest = new ChatRequestImpl();
    }

    @AfterEach
    void tearDown() {
        mockedServerTools.close();
    }

    @Test
    void userChatRequests_emptyMessage_returnsTrueAndSkipsProcessing() {
        user mockUser = mock(user.class);

        boolean result = chatRequest.userChatRequests(mockUser, "");

        assertTrue(result);
        verify(mockPluginManager, never()).callEvent(Mockito.any());
    }

    @Test
    void userChatRequests_nullMessage_throwsNullPointerException() {
        user mockUser = mock(user.class);

        assertThrows(NullPointerException.class, () ->
                chatRequest.userChatRequests(mockUser, null));
    }

    @Test
    void userChatRequests_regularMessage_firesChatEvent() {
        user mockUser = mock(user.class);

        chatRequest.userChatRequests(mockUser, "Hello, world!");

        verify(mockPluginManager).callEvent(Mockito.argThat(event ->
                event instanceof UserChatEvent
                        && ((UserChatEvent) event).getUserData() == mockUser
                        && ((UserChatEvent) event).getChatMessage().equals("Hello, world!")
        ));
    }

    @Test
    void userChatRequests_chatEventCancelled_returnsTrue() {
        user mockUser = mock(user.class);
        // When callEvent is invoked, cancel the event
        doAnswer(invocation -> {
            UserChatEvent event = invocation.getArgument(0);
            event.setCancelled(true);
            return null;
        }).when(mockPluginManager).callEvent(Mockito.any(UserChatEvent.class));

        boolean result = chatRequest.userChatRequests(mockUser, "message");

        assertTrue(result);
    }

    @Test
    void userChatRequests_chatEventNotCancelled_returnsFalse() {
        user mockUser = mock(user.class);

        boolean result = chatRequest.userChatRequests(mockUser, "message");

        assertFalse(result);
    }

    @Test
    void userChatRequests_commandMessage_firesCommandEvent() {
        user mockUser = mock(user.class);

        chatRequest.userChatRequests(mockUser, "/help");

        verify(mockPluginManager).callEvent(Mockito.argThat(event ->
                event instanceof UserCommandEvent
                        && ((UserCommandEvent) event).getCommand().equals("help")
        ));
    }

    @Test
    void userChatRequests_commandWithArgs_parsesCorrectly() {
        user mockUser = mock(user.class);

        chatRequest.userChatRequests(mockUser, "/tell Alice Hello there");

        verify(mockPluginManager).callEvent(Mockito.argThat(event ->
                event instanceof UserCommandEvent
                        && ((UserCommandEvent) event).getCommand().equals("tell")
                        && ((UserCommandEvent) event).getArgs()[0].equals("Alice")
                        && ((UserCommandEvent) event).getArgs()[1].equals("Hello")
                        && ((UserCommandEvent) event).getArgs()[2].equals("there")
        ));
    }

    @Test
    void userChatRequests_commandEventCancelled_returnsTrue() {
        user mockUser = mock(user.class);
        doAnswer(invocation -> {
            UserCommandEvent event = invocation.getArgument(0);
            event.setCancelled(true);
            return null;
        }).when(mockPluginManager).callEvent(Mockito.any(UserCommandEvent.class));

        boolean result = chatRequest.userChatRequests(mockUser, "/help");

        assertTrue(result);
        // When cancelled, should NOT try to execute the command (no sendMessageToUser)
        verify(mockApi, never()).sendMessageToUser(Mockito.any(), Mockito.anyString());
    }

    @Test
    void commandRequest_unknownCommand_sendsErrorMessage() {
        user mockUser = mock(user.class);

        chatRequest.commandRequest("nonexistent", new String[0], mockUser);

        verify(mockApi).sendMessageToUser(mockUser, "未知的命令！请输入/help查看帮助！");
    }

    @Test
    void commandRequest_knownCommand_executesSuccessfully() {
        user mockUser = mock(user.class);
        when(mockUser.getUserName()).thenReturn("TestUser");

        chatRequest.commandRequest("about", new String[0], mockUser);

        // about command should succeed without sending error messages
        verify(mockApi, never()).sendMessageToUser(eq(mockUser), Mockito.contains("语法错误"));
        verify(mockApi, never()).sendMessageToUser(eq(mockUser), Mockito.contains("未知的命令"));
    }

    @Test
    void getRegisterCommands_returnsUnmodifiableList() {
        assertEquals(17, chatRequest.getRegisterCommands().size());
        assertThrows(UnsupportedOperationException.class, () ->
                chatRequest.getRegisterCommands().clear());
    }

    @Test
    void registerCommand_validCommand_registersSuccessfully() {
        org.yuezhikong.Server.command.Command mockCommand = mock(org.yuezhikong.Server.command.Command.class);
        org.yuezhikong.Server.plugin.plugin.Plugin mockPlugin = mock(org.yuezhikong.Server.plugin.plugin.Plugin.class);
        ChatRequest.CommandInformation info = new ChatRequest.CommandInformation(mockPlugin, mockCommand, "test");

        chatRequest.registerCommand(info);

        assertTrue(chatRequest.getRegisterCommands().contains(info));
    }

    @Test
    void registerCommand_nullInformation_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                chatRequest.registerCommand(null));
    }

    @Test
    void registerCommand_nullPlugin_throwsIllegalArgumentException() {
        org.yuezhikong.Server.command.Command mockCommand = mock(org.yuezhikong.Server.command.Command.class);
        ChatRequest.CommandInformation info = new ChatRequest.CommandInformation(null, mockCommand, "test");

        assertThrows(IllegalArgumentException.class, () ->
                chatRequest.registerCommand(info));
    }

    @Test
    void unregisterCommand_byPlugin_removesAllPluginCommands() {
        org.yuezhikong.Server.command.Command mockCommand = mock(org.yuezhikong.Server.command.Command.class);
        org.yuezhikong.Server.plugin.plugin.Plugin mockPlugin = mock(org.yuezhikong.Server.plugin.plugin.Plugin.class);
        ChatRequest.CommandInformation info = new ChatRequest.CommandInformation(mockPlugin, mockCommand, "testcmd");

        chatRequest.registerCommand(info);
        assertEquals(18, chatRequest.getRegisterCommands().size());

        chatRequest.unregisterCommand(mockPlugin);
        assertEquals(17, chatRequest.getRegisterCommands().size());
    }
}
