package com.sportsbetting.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock
    private OutboxDispatcher outboxDispatcher;

    @Test
    void pollInvokesDispatcher() {
        OutboxPoller poller = new OutboxPoller(outboxDispatcher);
        poller.poll();
        verify(outboxDispatcher).dispatchPending();
    }

    @Test
    void pollSwallowsExceptions() {
        doThrow(new RuntimeException("boom")).when(outboxDispatcher).dispatchPending();
        OutboxPoller poller = new OutboxPoller(outboxDispatcher);
        poller.poll();
        verify(outboxDispatcher).dispatchPending();
    }
}
